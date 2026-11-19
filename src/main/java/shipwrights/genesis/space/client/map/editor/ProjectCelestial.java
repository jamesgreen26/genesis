package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.TextureSlot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// A celestial being made in the editor: the JSON of each of its {@linkplain CelestialDocuments documents}, edited in
/// place. Its textures are kept by the {@link CelestialProject}, by where they go.
public final class ProjectCelestial {
    private ResourceLocation id;
    private final Map<CelestialDocuments.Document<?>, JsonElement> documents = new LinkedHashMap<>();

    public ProjectCelestial(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    /// Only by {@link CelestialProject#rename}, which keeps its celestials by id. Changed in place, rather than made
    /// afresh, so whatever's editing it carries on.
    void setId(ResourceLocation id) {
        this.id = id;
    }

    public JsonElement json(CelestialDocuments.Document<?> document) {
        return documents.get(document);
    }

    public void setJson(CelestialDocuments.Document<?> document, JsonElement json) {
        documents.put(document, json);
    }

    public <T> DataResult<T> decode(CelestialDocuments.Document<T> document) {
        JsonElement json = documents.get(document);
        if (json == null) return DataResult.error(() -> "Missing");
        return document.decode(json);
    }

    /// The textures it's drawn with, as far as its appearance can be read
    public List<TextureSlot> textureSlots() {
        return appearance().map(visualizer -> visualizer.textures(id)).orElse(List.of());
    }

    public Optional<CelestialVisualizer> appearance() {
        return decode(CelestialDocuments.APPEARANCE).result();
    }
}
