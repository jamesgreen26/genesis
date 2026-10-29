package shipwrights.genesis.space.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.components.AtmosphereComponent;
import shipwrights.genesis.space.client.components.ShadedCubeComponent;
import shipwrights.genesis.space.client.components.StarComponent;
import shipwrights.genesis.util.ResourcePackRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/// How a celestial looks, loaded from {@code assets/<namespace>/genesis/celestial/<path>.json}. Shares its id with
/// the {@link shipwrights.genesis.space.Celestial} it depicts.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record CelestialVisualizer(List<Component> components) {
    public interface Component {
        Map<ResourceLocation, Codec<? extends Component>> REGISTRY = new HashMap<>();

        static void register(ResourceLocation type, Codec<? extends Component> codec) {
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

        event.registerReloadListener(REGISTRY);
    }
}
