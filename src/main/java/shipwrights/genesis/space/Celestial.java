package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.HashMap;
import java.util.Map;

/// @param size diameter in blocks
/// @param mass mass measured in blocks
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public record Celestial(
        boolean castsLight,
        boolean castsShadows,
        double size,
        double mass,
        TransformProvider transformProvider
) {
    public static final ResourceKey<Registry<Celestial>> REGISTRY_KEY
            = ResourceKey.createRegistryKey(GenesisMod.id("celestial"));

    @SubscribeEvent
    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        TransformProvider.register(StaticTransformProvider.TYPE, StaticTransformProvider.CODEC);
        TransformProvider.register(OrbitingTransformProvider.TYPE, OrbitingTransformProvider.CODEC);

        event.dataPackRegistry(REGISTRY_KEY, CODEC, CODEC);
    }

    Vector3dc getPosition(Level level) {
        return getPosition(level, 0f);
    }

    Quaterniondc getRotation(Level level) {
        return getRotation(level, 0f);
    }

    Vector3dc getPosition(Level level, float partialTick) {
        return transformProvider.getPosition(level.registryAccess().registryOrThrow(REGISTRY_KEY), Time.get(level, partialTick));
    }

    Quaterniondc getRotation(Level level, float partialTick) {
        return transformProvider.getRotation(level.registryAccess().registryOrThrow(REGISTRY_KEY), Time.get(level, partialTick));
    }

    public static final Codec<Celestial> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                Codec.BOOL.fieldOf("castsLight").forGetter(Celestial::castsLight),
                Codec.BOOL.fieldOf("castsShadows").forGetter(Celestial::castsShadows),
                Codec.DOUBLE.fieldOf("size").forGetter(Celestial::size),
                Codec.DOUBLE.fieldOf("mass").forGetter(Celestial::mass),
                TransformProvider.DISPATCH_CODEC.fieldOf("transformProvider").forGetter(Celestial::transformProvider)
        ).apply(instance, Celestial::new)
    );

    public interface TransformProvider {
        Vector3dc getPosition(Registry<Celestial> celestials, Time time);

        Quaterniondc getRotation(Registry<Celestial> celestials, Time time);

        ResourceLocation getType();

        Map<ResourceLocation, Codec<? extends TransformProvider>> REGISTRY = new HashMap<>();

        static void register(ResourceLocation type, Codec<? extends TransformProvider> codec) {
            REGISTRY.put(type, codec);
        }

        Codec<TransformProvider> DISPATCH_CODEC
                = ResourceLocation.CODEC.dispatchStable(TransformProvider::getType, REGISTRY::get);
    }
}
