package shipwrights.genesis.space.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.CodecRegistry;
import shipwrights.genesis.space.client.components.AtmosphereComponent;
import shipwrights.genesis.space.client.components.ShadedCubeComponent;
import shipwrights.genesis.space.client.components.StarComponent;
import shipwrights.genesis.space.client.components.StarGlowComponent;
import shipwrights.genesis.space.client.components.TexturedRingComponent;
import shipwrights.genesis.util.ResourcePackRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/// How a celestial looks, loaded from {@code assets/<namespace>/genesis/celestial/<path>.json}. Shares its id with
/// the {@link shipwrights.genesis.space.Celestial} it depicts.
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public record CelestialVisualizer(List<Component> components) {
    public interface Component {
        Map<ResourceLocation, MapCodec<? extends Component>> REGISTRY = new CodecRegistry<>();

        static void register(ResourceLocation type, MapCodec<? extends Component> codec) {
            REGISTRY.put(type, codec);
        }

        Codec<Component> DISPATCH_CODEC
                = ResourceLocation.CODEC.dispatchStable(Component::getType, REGISTRY::get);

        ResourceLocation getType();

        void render(CelestialRenderContext context);

        /// Whether this is drawn over the sky rather than behind it, so the sky brightens it instead of hiding it
        default boolean shinesThroughSky() {
            return false;
        }

        /// The textures this draws the celestial {@code celestialId} with, which aren't named in its JSON
        default List<TextureSlot> textures(ResourceLocation celestialId) {
            return List.of();
        }
    }

    private static final List<Runnable> TEXTURE_LISTENERS = new CopyOnWriteArrayList<>();

    /// Every texture the components draw the celestial {@code celestialId} with, once each
    public List<TextureSlot> textures(ResourceLocation celestialId) {
        Map<ResourceLocation, TextureSlot> slots = new LinkedHashMap<>();
        for (Component component : components) {
            for (TextureSlot slot : component.textures(celestialId)) slots.putIfAbsent(slot.location(), slot);
        }
        return List.copyOf(slots.values());
    }

    /// Runs {@code listener} whenever celestial textures are put in place or taken away other than by a resource
    /// reload, as editors do to preview them. For components that remember what textures there are.
    public static void onTexturesChanged(Runnable listener) {
        TEXTURE_LISTENERS.add(listener);
    }

    /// Tells {@linkplain #onTexturesChanged listeners} that celestial textures have been put in place or taken away
    public static void texturesChanged() {
        TEXTURE_LISTENERS.forEach(Runnable::run);
    }

    public void render(CelestialRenderContext context, Predicate<Component> filter) {
        for (var component : components()) {
            if (filter.test(component)) component.render(context);
        }
    }

    public static final Codec<CelestialVisualizer> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Component.DISPATCH_CODEC.listOf().fieldOf("components").forGetter(CelestialVisualizer::components)
            ).apply(instance, CelestialVisualizer::new)
    );

    public static final ResourceKey<Registry<CelestialVisualizer>> REGISTRY_KEY
            = ResourceKey.createRegistryKey(GenesisMod.id("celestial"));

    // Must come after CODEC, which it captures during static initialization
    public static final ResourcePackRegistry<CelestialVisualizer> REGISTRY = new ResourcePackRegistry<>(REGISTRY_KEY, CODEC);

    @SubscribeEvent
    public static void registerResourcePackRegistries(RegisterClientReloadListenersEvent event) {
        Component.register(AtmosphereComponent.TYPE, AtmosphereComponent.CODEC);
        Component.register(ShadedCubeComponent.TYPE, ShadedCubeComponent.CODEC);
        Component.register(StarComponent.TYPE, StarComponent.CODEC);
        Component.register(StarGlowComponent.TYPE, StarGlowComponent.CODEC);
        Component.register(TexturedRingComponent.TYPE, TexturedRingComponent.CODEC);

        event.registerReloadListener(REGISTRY);
    }
}
