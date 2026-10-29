package shipwrights.genesis.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;

/// The client-side counterpart to a datapack registry: entries are loaded from resource packs instead of datapacks,
/// and reloaded along with other client resources (F3+T).
///
/// Files are laid out the same way as Forge datapack registries, so a registry keyed {@code genesis:celestial}
/// loads {@code assets/<namespace>/genesis/celestial/<path>.json} as entry {@code <namespace>:<path>}.
///
/// Like datapack registries, each owner registers its own, by handing it to
/// {@link RegisterClientReloadListenersEvent#registerReloadListener} from a client-side mod bus subscriber.
public final class ResourcePackRegistry<T> extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final ResourceKey<Registry<T>> key;
    private final Codec<T> codec;
    private volatile Registry<T> registry;

    public ResourcePackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        super(GSON, ForgeHooks.prefixNamespace(key.location()));
        this.key = key;
        this.codec = codec;
        this.registry = freeze(new MappedRegistry<>(key, Lifecycle.stable()));
    }

    /// The entries from the most recent resource reload. Empty until resources have first loaded.
    public Registry<T> registry() {
        return registry;
    }

    public ResourceKey<Registry<T>> key() {
        return key;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var loaded = new MappedRegistry<>(key, Lifecycle.stable());
        jsons.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("Couldn't load {} entry {}: {}", key.location(), id, error))
                .ifPresent(entry -> loaded.register(ResourceKey.create(key, id), entry, Lifecycle.stable())));
        registry = freeze(loaded);
        LOGGER.info("Loaded {} {} entries", registry.size(), key.location());
    }

    private static <T> Registry<T> freeze(MappedRegistry<T> registry) {
        registry.freeze();
        return registry;
    }
}
