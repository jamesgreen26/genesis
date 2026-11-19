package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import shipwrights.genesis.space.client.TextureSlot;
import shipwrights.genesis.space.client.map.OfflineCelestials;
import shipwrights.genesis.util.schema.SchemaSeeds;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/// What celestials start out as: blank for new ones, and as the game's own packs have them for the game's own
final class Templates {
    private Templates() {
    }

    /// The simplest JSON of the document's shape, which may still need filling in to be valid
    static JsonElement blank(CelestialDocuments.Document<?> document) {
        return SchemaSeeds.fitted(document.codec(), document.schema());
    }

    /// A new celestial {@code id}, with every document blank
    static ProjectCelestial blankCelestial(ResourceLocation id) {
        ProjectCelestial celestial = new ProjectCelestial(id);
        for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) celestial.setJson(document, blank(document));
        return celestial;
    }

    /// The game's own celestials, as its packs have them. A document one hasn't got is blank, so it isn't taken for a
    /// change.
    static Map<ResourceLocation, CelestialProject.Defaults> defaults(OfflineCelestials builtIn) {
        Map<ResourceLocation, CelestialProject.Defaults> defaults = new LinkedHashMap<>();
        for (ResourceLocation id : builtIn.json().keySet()) {
            Map<CelestialDocuments.Document<?>, JsonElement> documents = new LinkedHashMap<>();
            ProjectCelestial celestial = new ProjectCelestial(id);
            for (CelestialDocuments.Document<?> document : CelestialDocuments.ALL) {
                JsonElement json = inPacks(document, id, builtIn).orElseGet(() -> blank(document));
                documents.put(document, json);
                celestial.setJson(document, json);
            }
            Map<ResourceLocation, byte[]> textures = new LinkedHashMap<>();
            for (TextureSlot slot : celestial.textureSlots()) {
                readResource(slot.location()).ifPresent(png -> textures.put(slot.location(), png));
            }
            defaults.put(id, new CelestialProject.Defaults(Map.copyOf(documents), Map.copyOf(textures)));
        }
        return defaults;
    }

    /// The document of {@code id} as the game's own packs have it
    private static Optional<JsonElement> inPacks(CelestialDocuments.Document<?> document, ResourceLocation id,
                                                 OfflineCelestials builtIn) {
        if (document == CelestialDocuments.DATA) return Optional.ofNullable(builtIn.json().get(id));
        if (document.packType() == PackType.CLIENT_RESOURCES) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(document.resource(id));
            if (resource.isEmpty()) return Optional.empty();
            try (Reader reader = resource.get().openAsReader()) {
                return Optional.of(JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<byte[]> readResource(ResourceLocation location) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) return Optional.empty();
        try (InputStream in = resource.get().open()) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
