package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shipwrights.genesis.MinecraftBootstrap;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.OrbitingTransformProvider;
import shipwrights.genesis.space.StaticTransformProvider;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.components.ShadedCubeComponent;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings;
import shipwrights.genesis.util.schema.Schema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialProjectTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("custom", "planets/test");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("genesis", "textures/celestials/custom/planets/test.png");

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
        CelestialVisualizer.Component.REGISTRY.putIfAbsent(ShadedCubeComponent.TYPE, ShadedCubeComponent.CODEC);
    }

    /// One celestial, drawn as a shaded cube, so with {@link #TEXTURE} as its surface
    private static CelestialProject projectWithOne(Path workspace) {
        CelestialProject project = CelestialProject.load(workspace, Map.of());
        ProjectCelestial celestial = new ProjectCelestial(ID);
        JsonObject data = JsonParser.parseString("{\"size\": 100, \"mass\": 5}").getAsJsonObject();
        celestial.setJson(CelestialDocuments.DATA, data);
        celestial.setJson(CelestialDocuments.APPEARANCE,
                JsonParser.parseString("{\"components\": [{\"type\": \"genesis:shaded_cube\", \"drawShadows\": false}]}"));
        project.add(celestial);
        project.textures().put(TEXTURE, new byte[]{1, 2, 3});
        return project;
    }

    @Test
    void restoringASnapshotPutsBackWhatWasChangedInPlaceOrRemoved(@TempDir Path workspace) {
        CelestialProject project = projectWithOne(workspace);
        CelestialProject.Snapshot before = project.snapshot();

        // As the form edits it: in place
        project.get(ID).orElseThrow().json(CelestialDocuments.DATA).getAsJsonObject().addProperty("size", 999);
        CelestialProject.Snapshot changed = project.snapshot();
        project.remove(project.get(ID).orElseThrow());
        assertTrue(project.get(ID).isEmpty());

        project.restore(before);
        ProjectCelestial restored = project.get(ID).orElseThrow();
        assertEquals(100, restored.json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());
        assertArrayEquals(new byte[]{1, 2, 3}, project.textures().get(TEXTURE));

        // Changing what was restored leaves the snapshot as it was
        restored.json(CelestialDocuments.DATA).getAsJsonObject().addProperty("size", 5);
        project.restore(before);
        assertEquals(100, project.get(ID).orElseThrow().json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());

        project.restore(changed);
        assertEquals(999, project.get(ID).orElseThrow().json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());
    }

    private static Map<String, byte[]> zipEntries(Path zip) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) entries.put(entry.getName(), in.readAllBytes());
        }
        return entries;
    }

    @Test
    void texturesNothingIsDrawnWithAreKeptButNotExported(@TempDir Path workspace, @TempDir Path exports) throws IOException {
        CelestialProject project = projectWithOne(workspace);
        // No longer a shaded cube, so nothing looks for its texture
        project.get(ID).orElseThrow().setJson(CelestialDocuments.APPEARANCE, JsonParser.parseString("{\"components\": []}"));

        Map<String, byte[]> entries = zipEntries(project.export(exports, "test"));
        assertTrue(entries.containsKey("assets/custom/genesis/celestial/planets/test.json"));
        assertFalse(entries.containsKey("assets/genesis/textures/celestials/custom/planets/test.png"));

        // Still there should it be made a shaded cube again
        project.save();
        assertArrayEquals(new byte[]{1, 2, 3}, CelestialProject.load(workspace, Map.of()).textures().get(TEXTURE));
    }

    @Test
    void howTexturesWereGeneratedIsKeptButNotExported(@TempDir Path workspace, @TempDir Path exports) throws IOException {
        CelestialProject project = projectWithOne(workspace);
        PlanetTextureSettings settings = PlanetTextureSettings.DEFAULT.withSeed(12).withBlend(0.3);
        project.generators().put(TEXTURE, settings);
        project.save();

        assertEquals(settings, CelestialProject.load(workspace, Map.of()).generators().get(TEXTURE));
        Map<String, byte[]> entries = zipEntries(project.export(exports, "test"));
        assertFalse(entries.containsKey(CelestialProject.GENERATORS_FILE));
        assertTrue(entries.containsKey("assets/genesis/textures/celestials/custom/planets/test.png"));
    }

    @Test
    void howATextureWasGeneratedFollowsItWhenRenamed(@TempDir Path workspace) {
        CelestialProject project = projectWithOne(workspace);
        PlanetTextureSettings settings = PlanetTextureSettings.DEFAULT;
        project.generators().put(TEXTURE, settings);
        project.rename(project.get(ID).orElseThrow(), ResourceLocation.fromNamespaceAndPath("custom", "renamed"));

        assertFalse(project.generators().containsKey(TEXTURE));
        assertEquals(settings, project.generators().get(
                ResourceLocation.fromNamespaceAndPath("genesis", "textures/celestials/custom/renamed.png")));
    }

    @Test
    void savingLeavesNothingBesideTheWorkspace(@TempDir Path parent) {
        Path workspace = parent.resolve("editor");
        CelestialProject project = projectWithOne(workspace);
        project.save();
        project.save();
        assertEquals(List.of(workspace), listing(parent));
    }

    @Test
    void aSaveStoppedPartwayLeavesTheLastOneWhole(@TempDir Path parent) throws IOException {
        Path workspace = parent.resolve("editor");
        projectWithOne(workspace).save();
        // As if stopped after the last save was moved aside, before the new one was moved in
        Files.move(workspace, parent.resolve("editor.previous"));
        Files.createDirectories(parent.resolve("editor.saving/data"));

        CelestialProject loaded = CelestialProject.load(workspace, Map.of());
        assertTrue(loaded.get(ID).isPresent());
        assertArrayEquals(new byte[]{1, 2, 3}, loaded.textures().get(TEXTURE));

        loaded.save();
        assertEquals(List.of(workspace), listing(parent));
    }

    private static List<Path> listing(Path folder) {
        try (var files = Files.list(folder)) {
            return files.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void savesAndLoadsInPackLayout(@TempDir Path workspace) throws IOException {
        projectWithOne(workspace).save();

        assertTrue(Files.exists(workspace.resolve("data/custom/genesis/celestial/planets/test.json")));
        assertTrue(Files.exists(workspace.resolve("assets/custom/genesis/celestial/planets/test.json")));
        assertArrayEquals(new byte[]{1, 2, 3},
                Files.readAllBytes(workspace.resolve("assets/genesis/textures/celestials/custom/planets/test.png")));

        CelestialProject loaded = CelestialProject.load(workspace, Map.of());
        assertEquals(1, loaded.celestials().size());
        ProjectCelestial celestial = loaded.get(ID).orElseThrow();
        assertEquals(100, celestial.json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());
        assertArrayEquals(new byte[]{1, 2, 3}, loaded.textures().get(TEXTURE));
    }

    @Test
    void savingReplacesWhatWasThere(@TempDir Path workspace) {
        CelestialProject project = projectWithOne(workspace);
        project.save();
        project.remove(project.get(ID).orElseThrow());
        project.save();
        assertTrue(CelestialProject.load(workspace, Map.of()).celestials().isEmpty());
        assertTrue(Files.notExists(workspace.resolve("data/custom/genesis/celestial/planets/test.json")));
    }

    @Test
    void exportsOneZipThatIsBothKindsOfPack(@TempDir Path workspace, @TempDir Path exports) throws IOException {
        Map<String, byte[]> entries = zipEntries(projectWithOne(workspace).export(exports, "test"));
        assertTrue(entries.containsKey("data/custom/genesis/celestial/planets/test.json"));
        assertTrue(entries.containsKey("assets/custom/genesis/celestial/planets/test.json"));
        assertTrue(entries.containsKey("assets/genesis/textures/celestials/custom/planets/test.png"));

        JsonObject pack = JsonParser.parseString(new String(entries.get("pack.mcmeta"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("pack");
        int min = pack.getAsJsonArray("supported_formats").get(0).getAsInt();
        int max = pack.getAsJsonArray("supported_formats").get(1).getAsInt();
        int resources = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        int data = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
        assertTrue(min <= resources && resources <= max);
        assertTrue(min <= data && data <= max);
        assertEquals(min, pack.get("pack_format").getAsInt());
    }

    @Test
    void theFirstExportExplainsHowToUseThem(@TempDir Path workspace, @TempDir Path exports) throws IOException {
        Path howToUse = exports.resolve(CelestialProject.HOW_TO_USE_FILE);
        CelestialProject project = projectWithOne(workspace);
        project.export(exports, "test");
        assertTrue(Files.readString(howToUse).contains("resource pack and a data pack"));

        // Left as it is after
        Files.writeString(howToUse, "edited");
        project.export(exports, "test");
        assertEquals("edited", Files.readString(howToUse));
    }

    private static final ResourceLocation SUN = ResourceLocation.fromNamespaceAndPath("genesis", "sun");

    private static ProjectCelestial orbiting(ResourceLocation id, String parent) {
        ProjectCelestial celestial = new ProjectCelestial(id);
        celestial.setJson(CelestialDocuments.DATA, JsonParser.parseString("""
                {"castsLight": false, "castsShadows": false, "size": 10, "mass": 1, "transformProvider":
                  {"type": "genesis:orbiting", "parent": "%s", "distance": 1000, "period": 24000}}
                """.formatted(parent)));
        celestial.setJson(CelestialDocuments.APPEARANCE, JsonParser.parseString("{\"components\": []}"));
        return celestial;
    }

    private static CelestialProject projectAroundSun(Path workspace) {
        Celestial.TransformProvider.REGISTRY.putIfAbsent(ResourceLocation.fromNamespaceAndPath("genesis", "static"),
                StaticTransformProvider.CODEC);
        Celestial.TransformProvider.REGISTRY.putIfAbsent(ResourceLocation.fromNamespaceAndPath("genesis", "orbiting"),
                OrbitingTransformProvider.CODEC);
        return CelestialProject.load(workspace, Map.of(SUN, sunDefaults()));
    }

    private static CelestialProject.Defaults sunDefaults() {
        return new CelestialProject.Defaults(Map.of(
                CelestialDocuments.DATA, JsonParser.parseString("""
                        {"castsLight": true, "castsShadows": false, "size": 100, "mass": 1000, "transformProvider":
                          {"type": "genesis:static", "position": [0, 0, 0], "rotation": [0, 0, 0]}}
                        """),
                CelestialDocuments.APPEARANCE, JsonParser.parseString("{\"components\": []}")
        ), Map.of());
    }

    private static List<String> problemKeys(CelestialProject project, ProjectCelestial celestial) {
        return project.problems(celestial).stream()
                .map(problem -> problem.getContents() instanceof TranslatableContents contents ? contents.getKey() : problem.getString())
                .toList();
    }

    @Test
    void orbitingWhatExistsIsFine(@TempDir Path workspace) {
        CelestialProject project = projectAroundSun(workspace);
        ProjectCelestial planet = orbiting(ID, SUN.toString());
        project.add(planet);
        assertEquals(List.of(), problemKeys(project, planet));
        assertTrue(project.isExportable());
    }

    @Test
    void orbitingWhatIsMissingOrItselfIsAProblem(@TempDir Path workspace) {
        CelestialProject project = projectAroundSun(workspace);
        ProjectCelestial missing = orbiting(ID, "custom:nowhere");
        ProjectCelestial itself = orbiting(ResourceLocation.fromNamespaceAndPath("custom", "itself"), "custom:itself");
        project.add(missing);
        project.add(itself);

        assertEquals(List.of("genesis.editor.problem.unknown_id"), problemKeys(project, missing));
        assertEquals(List.of("genesis.editor.problem.unknown_id"), problemKeys(project, itself));
        assertFalse(project.isExportable());
    }

    @Test
    void orbitingInALoopCantBePlaced(@TempDir Path workspace) {
        CelestialProject project = projectAroundSun(workspace);
        ResourceLocation a = ResourceLocation.fromNamespaceAndPath("custom", "a");
        ResourceLocation b = ResourceLocation.fromNamespaceAndPath("custom", "b");
        ProjectCelestial first = orbiting(a, b.toString());
        project.add(first);
        project.add(orbiting(b, a.toString()));

        assertEquals(List.of("genesis.editor.problem.unplaceable"), problemKeys(project, first));
    }

    @Test
    void builtInCelestialsAreEditableButOnlyExportedOnceChanged(@TempDir Path workspace, @TempDir Path exports) throws IOException {
        CelestialProject project = projectAroundSun(workspace);
        ProjectCelestial sun = project.get(SUN).orElseThrow();
        assertTrue(project.isBuiltIn(sun));
        assertFalse(project.isChanged(sun));
        // Nothing to export yet
        assertFalse(project.isExportable());

        sun.json(CelestialDocuments.DATA).getAsJsonObject().addProperty("size", 200);
        assertTrue(project.isChanged(sun));
        assertTrue(project.isExportable());

        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(project.export(exports, "test")))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) entries.put(entry.getName(), in.readAllBytes());
        }
        // Only the file that changed
        assertTrue(entries.containsKey("data/genesis/genesis/celestial/sun.json"));
        assertFalse(entries.containsKey("assets/genesis/genesis/celestial/sun.json"));

        // Kept as changed, and the rest filled back in, when loaded again
        project.save();
        CelestialProject loaded = CelestialProject.load(workspace, Map.of(SUN, sunDefaults()));
        ProjectCelestial loadedSun = loaded.get(SUN).orElseThrow();
        assertEquals(200, loadedSun.json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());
        assertEquals(sunDefaults().documents().get(CelestialDocuments.APPEARANCE), loadedSun.json(CelestialDocuments.APPEARANCE));

        loaded.reset(loadedSun);
        assertFalse(loaded.isChanged(loadedSun));
        assertEquals(100, loadedSun.json(CelestialDocuments.DATA).getAsJsonObject().get("size").getAsInt());
    }

    @Test
    void renamingMovesTexturesAndWhatReferredToIt(@TempDir Path workspace) {
        CelestialProject project = projectAroundSun(workspace);
        ProjectCelestial planet = orbiting(ID, SUN.toString());
        planet.setJson(CelestialDocuments.APPEARANCE,
                JsonParser.parseString("{\"components\": [{\"type\": \"genesis:shaded_cube\", \"drawShadows\": false}]}"));
        project.add(planet);
        project.add(orbiting(ResourceLocation.fromNamespaceAndPath("custom", "moon"), ID.toString()));
        project.textures().put(TEXTURE, new byte[]{1, 2, 3});

        ResourceLocation renamed = ResourceLocation.fromNamespaceAndPath("custom", "renamed");
        project.rename(planet, renamed);

        assertEquals(renamed, planet.id());
        assertTrue(project.get(ID).isEmpty());
        assertTrue(project.get(renamed).isPresent());
        assertFalse(project.textures().containsKey(TEXTURE));
        assertArrayEquals(new byte[]{1, 2, 3}, project.textures().get(
                ResourceLocation.fromNamespaceAndPath("genesis", "textures/celestials/custom/renamed.png")));
        // Its moon still orbits it
        ProjectCelestial moon = project.get(ResourceLocation.fromNamespaceAndPath("custom", "moon")).orElseThrow();
        assertEquals(renamed.toString(), moon.json(CelestialDocuments.DATA).getAsJsonObject()
                .getAsJsonObject("transformProvider").get("parent").getAsString());
        assertEquals(List.of(), problemKeys(project, moon));
    }

    @Test
    void theGameOwnCelestialsCantBeRenamed(@TempDir Path workspace) {
        CelestialProject project = projectAroundSun(workspace);
        ProjectCelestial sun = project.get(SUN).orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> project.rename(sun, ResourceLocation.fromNamespaceAndPath("custom", "sun")));
        assertEquals(SUN, sun.id());
    }

    @Test
    void unusedIdsAreNumberedPastThoseTaken(@TempDir Path workspace) {
        CelestialProject project = CelestialProject.load(workspace, Map.of());
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath("example", "new_celestial");
        assertEquals(base, project.unusedId(base));
        project.add(Templates.blankCelestial(base));
        assertEquals(base.withSuffix("_2"), project.unusedId(base));
        project.add(Templates.blankCelestial(base.withSuffix("_2")));
        assertEquals(base.withSuffix("_3"), project.unusedId(base));
    }

    @Test
    void appearanceSchemaListsEveryComponent() {
        var schema = CelestialDocuments.APPEARANCE.schema();
        var components = (Schema.ListSchema) ((Schema.MapSchema) schema)
                .fields().get(0).schema();
        var dispatch = (Schema.DispatchSchema) components.element();
        assertTrue(dispatch.options().keySet().containsAll(CelestialVisualizer.Component.REGISTRY.keySet()),
                () -> dispatch.options().keySet() + " vs " + CelestialVisualizer.Component.REGISTRY.keySet());
    }
}
