package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.ForgeHooks;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.util.schema.Schema;
import shipwrights.genesis.util.schema.SchemaProbe;

import java.util.List;

/// The JSON files that make up a celestial, each read by a codec from a registry folder of a pack. The editor works
/// from this list, so a celestial gaining another file only needs another entry here.
public final class CelestialDocuments {
    /// How the celestial behaves: its size, mass, orbit and so on. In a data pack.
    public static final Document<Celestial> DATA = new Document<>("data", PackType.SERVER_DATA,
            Celestial.REGISTRY_KEY, Celestial.CODEC);
    /// How the celestial looks. In a resource pack.
    public static final Document<CelestialVisualizer> APPEARANCE = new Document<>("appearance", PackType.CLIENT_RESOURCES,
            CelestialVisualizer.REGISTRY_KEY, CelestialVisualizer.CODEC);

    public static final List<Document<?>> ALL = List.of(DATA, APPEARANCE);

    private CelestialDocuments() {
    }

    /// @param name        identifies it, in the editor's translation keys
    /// @param packType    the kind of pack it's in
    /// @param registryKey the registry whose folder it's in
    public static final class Document<T> {
        private final String name;
        private final PackType packType;
        private final ResourceKey<? extends Registry<T>> registryKey;
        private final Codec<T> codec;
        private final FileToIdConverter files;
        private Schema schema;

        Document(String name, PackType packType, ResourceKey<? extends Registry<T>> registryKey, Codec<T> codec) {
            this.name = name;
            this.packType = packType;
            this.registryKey = registryKey;
            this.codec = codec;
            this.files = FileToIdConverter.json(ForgeHooks.prefixNamespace(registryKey.location()));
        }

        public Component title() {
            return Component.translatable("genesis.editor.document." + name);
        }

        public PackType packType() {
            return packType;
        }

        public Codec<T> codec() {
            return codec;
        }

        /// What the codec reads, worked out the first time it's asked for. Types registered by then are included,
        /// which is all of them, as they're registered while the game loads.
        public Schema schema() {
            if (schema == null) schema = SchemaProbe.infer(codec);
            return schema;
        }

        /// The registry's folder within a namespace, as {@code genesis/celestial}
        public String folder() {
            return ForgeHooks.prefixNamespace(registryKey.location());
        }

        /// Where the file for {@code id} goes in a pack, from the pack's root, as {@code data/<ns>/genesis/celestial/<path>.json}
        public String path(ResourceLocation id) {
            ResourceLocation file = files.idToFile(id);
            return packType.getDirectory() + "/" + file.getNamespace() + "/" + file.getPath();
        }

        /// The file for {@code id} within the packs a resource manager reads, as {@code <ns>:genesis/celestial/<path>.json}
        public ResourceLocation resource(ResourceLocation id) {
            return files.idToFile(id);
        }

        public DataResult<T> decode(JsonElement json) {
            try {
                return codec.parse(JsonOps.INSTANCE, json);
            } catch (RuntimeException e) {
                return DataResult.error(() -> String.valueOf(e.getMessage()));
            }
        }
    }
}
