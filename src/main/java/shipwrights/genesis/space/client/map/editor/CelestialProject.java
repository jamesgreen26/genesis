package shipwrights.genesis.space.client.map.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.client.map.OfflineCelestials;
import shipwrights.genesis.util.schema.SchemaReferences;
import shipwrights.genesis.space.client.TextureSlot;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/// The celestials being made in the editor, and their textures: those of the game's own packs, as editable as any, and
/// new ones. Kept in a workspace folder laid out as a pack with both {@code assets} and {@code data}, so it's what an
/// export zips up.
///
/// Only what differs from the game's own is kept or exported: new celestials, and the files of built-in ones that
/// have been changed, which then override the game's own as any pack's would. The rest is filled back in from the
/// {@linkplain Defaults defaults} when loaded.
public final class CelestialProject {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter EXPORT_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    /// Beside the workspace: where a save's written before it replaces it, and where the last save's kept meanwhile
    private static final String STAGING_SUFFIX = ".saving";
    private static final String PREVIOUS_SUFFIX = ".previous";
    /// Put beside the exports, explaining what to do with them
    static final String HOW_TO_USE_FILE = "HOW_TO_USE.txt";
    /// In the workspace, beside the pack's folders: how each generated texture was made. Not exported, as it's only of
    /// use to the editor.
    static final String GENERATORS_FILE = "texture_generators.json";
    private static final String HOW_TO_USE = """
            Celestials exported from Genesis's celestial editor
            ===================================================

            Each zip here is both a resource pack and a data pack. Celestials are made of two halves:

              - assets/ (client): how they look - their visuals and textures
              - data/   (server): how they behave - size, mass, orbit and so on

            So a zip has to be loaded BOTH ways for its celestials to work. Loaded only one way, they'll be missing
            or invisible.

            In single player:
              1. Put the zip in the resourcepacks folder, and turn it on under Options > Resource Packs.
              2. Put the same zip in the world's datapacks folder. For a new world, use the Data Packs button on
                 the Create World screen; for an existing one, copy it to saves/<world>/datapacks and run /reload
                 or reopen the world.

            On a server:
              1. Put the zip in the world's datapacks folder on the server.
              2. Have every player load it as a resource pack too, or send it as the server resource pack.
            """;

    private final Path workspace;
    private final Map<ResourceLocation, ProjectCelestial> celestials = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
    /// PNGs, by the location of the texture they are
    private final Map<ResourceLocation, byte[]> textures = new LinkedHashMap<>();
    /// The game's own celestials as its packs have them, by id
    private final Map<ResourceLocation, Defaults> defaults;
    /// Every texture of the game's own celestials, by location
    private final Map<ResourceLocation, byte[]> defaultTextures = new HashMap<>();
    /// How each texture that was generated was last made, by its location, so it can be made again with changes
    private final Map<ResourceLocation, PlanetTextureSettings> generators = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));

    /// A celestial as the game's own packs have it
    ///
    /// @param documents its JSON, for each of its documents
    /// @param textures  the PNGs of its textures, by location
    public record Defaults(Map<CelestialDocuments.Document<?>, JsonElement> documents, Map<ResourceLocation, byte[]> textures) {}

    private CelestialProject(Path workspace, Map<ResourceLocation, Defaults> defaults) {
        this.workspace = workspace;
        this.defaults = Map.copyOf(defaults);
        defaults.values().forEach(each -> defaultTextures.putAll(each.textures()));
    }

    public List<ProjectCelestial> celestials() {
        return List.copyOf(celestials.values());
    }

    public Optional<ProjectCelestial> get(ResourceLocation id) {
        return Optional.ofNullable(celestials.get(id));
    }

    public void add(ProjectCelestial celestial) {
        celestials.put(celestial.id(), celestial);
    }

    /// Removes {@code celestial}, and its textures
    public void remove(ProjectCelestial celestial) {
        celestial.textureSlots().forEach(slot -> {
            textures.remove(slot.location());
            generators.remove(slot.location());
        });
        celestials.remove(celestial.id());
    }

    public Map<ResourceLocation, byte[]> textures() {
        return textures;
    }

    /// How generated textures were made, by their location
    public Map<ResourceLocation, PlanetTextureSettings> generators() {
        return generators;
    }

    /// Gives {@code celestial}, a new one, the id {@code id}, which no other has. Its textures go to where they're
    /// looked for under its new id, and whatever referred to it, as celestials orbiting it, refers to it by that.
    public void rename(ProjectCelestial celestial, ResourceLocation id) {
        if (isBuiltIn(celestial)) throw new IllegalArgumentException("Can't rename the game's own " + celestial.id());
        if (celestials.containsKey(id)) throw new IllegalArgumentException("Already a celestial " + id);
        ResourceLocation old = celestial.id();

        List<TextureSlot> from = celestial.textureSlots();
        celestials.remove(old);
        celestial.setId(id);
        celestials.put(id, celestial);
        List<TextureSlot> to = celestial.textureSlots();
        // The same components look for them, so each goes to the same slot. Taken out first, then put back, in case
        // where one goes is where another was.
        Map<ResourceLocation, byte[]> moved = new LinkedHashMap<>();
        Map<ResourceLocation, PlanetTextureSettings> movedGenerators = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(from.size(), to.size()); i++) {
            byte[] png = textures.remove(from.get(i).location());
            if (png != null) moved.put(to.get(i).location(), png);
            PlanetTextureSettings generator = generators.remove(from.get(i).location());
            if (generator != null) movedGenerators.put(to.get(i).location(), generator);
        }
        textures.putAll(moved);
        generators.putAll(movedGenerators);

        for (ProjectCelestial each : celestials.values()) {
            for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
                JsonElement json = each.json(document);
                if (json == null) continue;
                each.setJson(document, SchemaReferences.rename(document.schema(), json, Celestial.REGISTRY_KEY, old, id));
            }
        }
    }

    // Undo

    /// The project as it is at some moment, to go back to: each celestial's JSON, and its textures and how they were
    /// generated. PNGs aren't copied, as they're only ever replaced, never changed in place.
    public record Snapshot(Map<ResourceLocation, Map<CelestialDocuments.Document<?>, JsonElement>> celestials,
                           Map<ResourceLocation, byte[]> textures,
                           Map<ResourceLocation, PlanetTextureSettings> generators) {}

    public Snapshot snapshot() {
        Map<ResourceLocation, Map<CelestialDocuments.Document<?>, JsonElement>> documents = new LinkedHashMap<>();
        for (ProjectCelestial celestial : celestials.values()) {
            Map<CelestialDocuments.Document<?>, JsonElement> each = new LinkedHashMap<>();
            for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
                JsonElement json = celestial.json(document);
                // Copied, as the editor changes JSON in place
                if (json != null) each.put(document, json.deepCopy());
            }
            documents.put(celestial.id(), each);
        }
        return new Snapshot(documents, new LinkedHashMap<>(textures), new LinkedHashMap<>(generators));
    }

    /// Puts the project back as it was at {@code snapshot}. Its celestials are made afresh, so whatever was editing
    /// them has to be made afresh too.
    public void restore(Snapshot snapshot) {
        celestials.clear();
        snapshot.celestials().forEach((id, documents) -> {
            ProjectCelestial celestial = new ProjectCelestial(id);
            // Copied again, so the snapshot's left as it was however this is changed
            documents.forEach((document, json) -> celestial.setJson(document, json.deepCopy()));
            celestials.put(id, celestial);
        });
        textures.clear();
        textures.putAll(snapshot.textures());
        generators.clear();
        generators.putAll(snapshot.generators());
    }

    /// {@code base}, or if a celestial has it already, the first of {@code base_2}, {@code base_3} and so on that none
    /// has
    public ResourceLocation unusedId(ResourceLocation base) {
        ResourceLocation id = base;
        for (int n = 2; celestials.containsKey(id); n++) id = base.withSuffix("_" + n);
        return id;
    }

    /// Whether {@code celestial} is one of the game's own, so can be reset rather than deleted
    public boolean isBuiltIn(ProjectCelestial celestial) {
        return defaults.containsKey(celestial.id());
    }

    /// Whether {@code celestial} differs from the game's own: always, for a new one
    public boolean isChanged(ProjectCelestial celestial) {
        Defaults original = defaults.get(celestial.id());
        if (original == null) return true;
        for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
            if (!Objects.equals(celestial.json(document), original.documents().get(document))) return true;
        }
        for (TextureSlot slot : celestial.textureSlots()) {
            if (isChangedTexture(slot.location())) return true;
        }
        return false;
    }

    /// Whether the game's own packs have a texture at {@code location}, for one of its celestials
    public boolean hasDefaultTexture(ResourceLocation location) {
        return defaultTextures.containsKey(location);
    }

    /// Puts the game's own texture back at {@code location}, if it has one there
    public void resetTexture(ResourceLocation location) {
        byte[] png = defaultTextures.get(location);
        if (png != null) textures.put(location, png.clone());
    }

    /// Whether the project has a texture at {@code location} unlike the game's own there, if it has one
    public boolean isChangedTexture(ResourceLocation location) {
        byte[] png = textures.get(location);
        return png != null && !Arrays.equals(png, defaultTextures.get(location));
    }

    /// Puts the game's own celestial {@code celestial} back as its packs have it
    public void reset(ProjectCelestial celestial) {
        Defaults original = defaults.get(celestial.id());
        if (original == null) return;
        celestial.textureSlots().forEach(slot -> {
            textures.remove(slot.location());
            generators.remove(slot.location());
        });
        original.documents().forEach((document, json) -> celestial.setJson(document, json.deepCopy()));
        original.textures().forEach((location, png) -> textures.put(location, png.clone()));
    }

    /// The ids there are in {@code registry}, other than {@code except}. Celestials are those of the game's packs and
    /// the project's; other registries, whatever's built into the game. Empty for a registry it doesn't know.
    public List<ResourceLocation> ids(ResourceKey<? extends Registry<?>> registry, @Nullable ResourceLocation except) {
        Set<ResourceLocation> ids = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        if (registry.equals(Celestial.REGISTRY_KEY)) {
            // The game's own among them
            ids.addAll(celestials.keySet());
        } else {
            BuiltInRegistries.REGISTRY.getOptional(registry.location()).ifPresent(entries -> ids.addAll(entries.keySet()));
        }
        if (except != null) ids.remove(except);
        return List.copyOf(ids);
    }

    // Problems

    /// What's wrong with {@code celestial}, if anything, such that it can't be exported:
    ///
    /// - each document that its codec can't read
    /// - each id in them of something that isn't there, as a parent that doesn't exist, or that's itself
    /// - failing those, if it can't be placed among the other celestials, as for orbiting in a loop
    /// - each texture it needs but hasn't got
    public List<Component> problems(ProjectCelestial celestial) {
        List<Component> problems = new ArrayList<>();
        for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
            celestial.decode(document).error().ifPresent(error -> problems.add(
                    Component.translatable("genesis.editor.problem.document", document.title(), error.message())));
        }
        for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
            JsonElement json = celestial.json(document);
            if (json == null) continue;
            for (SchemaReferences.Reference reference : SchemaReferences.find(document.schema(), json)) {
                List<ResourceLocation> known = ids(reference.registry(), celestial.id());
                ResourceLocation id = ResourceLocation.tryParse(reference.id());
                // A registry it knows nothing of can't be checked
                if (known.isEmpty() && !reference.registry().equals(Celestial.REGISTRY_KEY)) continue;
                if (reference.id().isBlank()) {
                    problems.add(Component.translatable("genesis.editor.problem.missing_id",
                            SchemaRows.fieldName(reference.field())));
                } else if (id == null || !known.contains(id)) {
                    problems.add(Component.translatable("genesis.editor.problem.unknown_id",
                            SchemaRows.fieldName(reference.field()), reference.id()));
                }
            }
        }
        if (problems.isEmpty() && !placeable(celestial)) {
            problems.add(Component.translatable("genesis.editor.problem.unplaceable"));
        }
        for (TextureSlot slot : celestial.textureSlots()) {
            if (slot.required() && !textures.containsKey(slot.location()) && !TextureSlot.exists(slot.location())) {
                problems.add(Component.translatable("genesis.editor.problem.texture", slot.name()));
            }
        }
        return problems;
    }

    /// Whether {@code celestial} can be placed among the rest of the project's celestials, the game's own among them
    private boolean placeable(ProjectCelestial celestial) {
        Map<ResourceLocation, Celestial> all = new LinkedHashMap<>();
        for (ProjectCelestial each : celestials.values()) {
            each.decode(CelestialDocuments.DATA).result().ifPresent(data -> all.put(each.id(), data));
        }
        return OfflineCelestials.placeable(all, Time.of(0, 0f)).containsKey(celestial.id());
    }

    /// Whether there's anything to export, and nothing wrong with any of it
    public boolean isExportable() {
        return !files(true).isEmpty() && celestials.values().stream().allMatch(celestial -> problems(celestial).isEmpty());
    }

    // Workspace

    /// The project in {@code workspace}, which needn't exist yet, with the game's own celestials as in
    /// {@code defaults} where it hasn't changed them
    public static CelestialProject load(Path workspace, Map<ResourceLocation, Defaults> defaults) {
        recoverInterruptedSave(workspace);
        CelestialProject project = new CelestialProject(workspace, defaults);
        for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
            Path root = workspace.resolve(document.packType().getDirectory());
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> namespaces = Files.list(root)) {
                for (Path namespace : namespaces.filter(Files::isDirectory).toList()) {
                    Path files = namespace.resolve(document.folder());
                    if (!Files.isDirectory(files)) continue;
                    try (Stream<Path> walk = Files.walk(files)) {
                        for (Path file : walk.filter(path -> path.toString().endsWith(".json")).toList()) {
                            String relative = files.relativize(file).toString().replace('\\', '/');
                            ResourceLocation id = ResourceLocation.tryBuild(namespace.getFileName().toString(),
                                    relative.substring(0, relative.length() - ".json".length()));
                            if (id == null) continue;
                            project.celestials.computeIfAbsent(id, ProjectCelestial::new)
                                    .setJson(document, JsonParser.parseString(Files.readString(file)));
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Couldn't load the celestial editor's {} from {}", document.title().getString(), root, e);
            }
        }

        // The game's own celestials, as far as they haven't been changed, and anything else missing afresh
        defaults.keySet().forEach(id -> project.celestials.computeIfAbsent(id, ProjectCelestial::new));
        for (ProjectCelestial celestial : project.celestials.values()) {
            Defaults original = defaults.get(celestial.id());
            for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
                if (celestial.json(document) != null) continue;
                JsonElement json = original == null ? null : original.documents().get(document);
                celestial.setJson(document, json == null ? Templates.blank(document) : json.deepCopy());
            }
        }

        Path assets = workspace.resolve(PackType.CLIENT_RESOURCES.getDirectory());
        if (Files.isDirectory(assets)) {
            try (Stream<Path> walk = Files.walk(assets)) {
                for (Path file : walk.filter(path -> path.toString().endsWith(".png")).toList()) {
                    Path relative = assets.relativize(file);
                    if (relative.getNameCount() < 2) continue;
                    ResourceLocation location = ResourceLocation.tryBuild(relative.getName(0).toString(),
                            relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/'));
                    if (location != null) project.textures.put(location, Files.readAllBytes(file));
                }
            } catch (IOException e) {
                LOGGER.error("Couldn't load the celestial editor's textures from {}", assets, e);
            }
        }
        project.defaultTextures.forEach((location, png) -> project.textures.putIfAbsent(location, png.clone()));
        project.loadGenerators(workspace.resolve(GENERATORS_FILE));
        return project;
    }

    private void loadGenerators(Path file) {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject all = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
                ResourceLocation location = ResourceLocation.tryParse(entry.getKey());
                if (location == null) continue;
                PlanetTextureSettings.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> LOGGER.warn("Couldn't read how {} was generated: {}", location, error))
                        .ifPresent(settings -> generators.put(location, settings));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Couldn't load how the celestial editor's textures were generated from {}", file, e);
        }
    }

    /// Writes the whole project to its workspace, replacing what was there.
    ///
    /// Written in full beside the workspace first, then swapped in for it, so that failing partway, or the game
    /// stopping, leaves the last save whole rather than half deleted.
    public void save() {
        Path staging = sibling(workspace, STAGING_SUFFIX);
        Path previous = sibling(workspace, PREVIOUS_SUFFIX);
        try {
            deleteTree(staging);
            Files.createDirectories(staging);
            for (Map.Entry<String, byte[]> file : files(false).entrySet()) {
                Path path = staging.resolve(file.getKey());
                Files.createDirectories(path.getParent());
                Files.write(path, file.getValue());
            }

            deleteTree(previous);
            if (Files.exists(workspace)) Files.move(workspace, previous, StandardCopyOption.ATOMIC_MOVE);
            Files.move(staging, workspace, StandardCopyOption.ATOMIC_MOVE);
            deleteTree(previous);
        } catch (IOException e) {
            LOGGER.error("Couldn't save the celestial editor's project to {}", workspace, e);
            // Whatever happened, the workspace mustn't be left missing while the last save's still about
            recoverInterruptedSave(workspace);
        }
    }

    /// Puts the last save back in place if saving was stopped between moving it aside and moving the new one in
    private static void recoverInterruptedSave(Path workspace) {
        Path previous = sibling(workspace, PREVIOUS_SUFFIX);
        if (Files.exists(workspace) || !Files.isDirectory(previous)) return;
        try {
            Files.move(previous, workspace, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.error("Couldn't restore the celestial editor's project from {}", previous, e);
        }
    }

    private static Path sibling(Path workspace, String suffix) {
        return workspace.resolveSibling(workspace.getFileName() + suffix);
    }

    /// Zips the project up, with a {@code pack.mcmeta} that makes it both a resource pack and a data pack, into
    /// {@code folder}
    ///
    /// @param name what to name the zip after, before the time it's made
    /// @return the zip
    public Path export(Path folder, String name) throws IOException {
        Files.createDirectories(folder);
        // Only while it isn't there, as on the first export, so it's never written over once changed
        Path howToUse = folder.resolve(HOW_TO_USE_FILE);
        if (Files.notExists(howToUse)) Files.writeString(howToUse, HOW_TO_USE, StandardCharsets.UTF_8);
        Path zip = folder.resolve(name + "_" + LocalDateTime.now().format(EXPORT_NAME) + ".zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
            zipOut.putNextEntry(new ZipEntry("pack.mcmeta"));
            zipOut.write(GSON.toJson(packMetadata()).getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            for (Map.Entry<String, byte[]> file : files(true).entrySet()) {
                zipOut.putNextEntry(new ZipEntry(file.getKey()));
                zipOut.write(file.getValue());
                zipOut.closeEntry();
            }
        }
        return zip;
    }

    /// Every file in the project that differs from the game's own, by its path from the pack's root: all those of new
    /// celestials, and those of the game's own that have been changed
    ///
    /// @param exporting whether only textures some celestial is drawn with are wanted. Saving keeps the rest too, so a
    ///                  texture isn't lost while what it's for is taken out of a celestial's visuals and put back, or
    ///                  while those visuals can't be read.
    private Map<String, byte[]> files(boolean exporting) {
        Set<ResourceLocation> used = exporting ? usedTextures() : Set.of();
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (ProjectCelestial celestial : celestials.values()) {
            Defaults original = defaults.get(celestial.id());
            for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
                JsonElement json = celestial.json(document);
                if (json == null || original != null && json.equals(original.documents().get(document))) continue;
                files.put(document.path(celestial.id()), GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
            }
        }
        textures.forEach((location, png) -> {
            if (exporting && !used.contains(location)) return;
            if (Arrays.equals(png, defaultTextures.get(location))) return;
            files.put(PackType.CLIENT_RESOURCES.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath(), png);
        });
        if (!exporting && !generators.isEmpty()) {
            JsonObject all = new JsonObject();
            generators.forEach((location, settings) -> PlanetTextureSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                    .resultOrPartial(error -> LOGGER.warn("Couldn't save how {} was generated: {}", location, error))
                    .ifPresent(json -> all.add(location.toString(), json)));
            files.put(GENERATORS_FILE, GSON.toJson(all).getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }

    /// Where every celestial's visuals look for textures, as far as they can be read
    private Set<ResourceLocation> usedTextures() {
        Set<ResourceLocation> used = new HashSet<>();
        celestials.values().forEach(celestial -> celestial.textureSlots().forEach(slot -> used.add(slot.location())));
        return used;
    }

    /// Accepted as both kinds of pack, by covering both pack formats
    private static JsonObject packMetadata() {
        int resources = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        int data = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Celestials made with Genesis");
        pack.addProperty("pack_format", Math.min(resources, data));
        JsonArray supported = new JsonArray();
        supported.add(Math.min(resources, data));
        supported.add(Math.max(resources, data));
        pack.add("supported_formats", supported);
        JsonObject metadata = new JsonObject();
        metadata.add("pack", pack);
        return metadata;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
