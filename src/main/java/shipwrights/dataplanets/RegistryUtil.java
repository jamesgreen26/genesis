package shipwrights.dataplanets;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class RegistryUtil {

    public static void registerBiome(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            Biome biome
    ) {
        registerThing(server, Registries.BIOME, ResourceKey.create(Registries.BIOME, resourceLocation), biome);
    }

    public static void registerDimensionType(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            DimensionType dimensionType
    ) {
        registerThing(server, Registries.DIMENSION_TYPE, ResourceKey.create(Registries.DIMENSION_TYPE, resourceLocation), dimensionType);
    }

    public static void registerLevelStem(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            LevelStem levelStem
    ) {
        registerThing(server, Registries.LEVEL_STEM, ResourceKey.create(Registries.LEVEL_STEM, resourceLocation), levelStem);
    }

    public static void registerConfiguredCarver(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredWorldCarver<?> configuredCarver
    ) {
        registerThing(server, Registries.CONFIGURED_CARVER, ResourceKey.create(Registries.CONFIGURED_CARVER, resourceLocation), configuredCarver);
    }

    public static void registerPlacedFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            PlacedFeature placedFeature
    ) {
        registerThing(server, Registries.PLACED_FEATURE, ResourceKey.create(Registries.PLACED_FEATURE, resourceLocation), placedFeature);
    }

    public static void registerConfiguredFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredFeature<?, ?> configuredFeature
    ) {
        registerThing(server, Registries.CONFIGURED_FEATURE, ResourceKey.create(Registries.CONFIGURED_FEATURE, resourceLocation), configuredFeature);
    }

    public static void registerNoise(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            NormalNoise.NoiseParameters noiseParameters
    ) {
        registerThing(server, Registries.NOISE, ResourceKey.create(Registries.NOISE, resourceLocation), noiseParameters);
    }

    @SuppressWarnings("deprecation")
    private static <T> void registerThing(
            MinecraftServer server,
            ResourceKey<Registry<T>> registry,
            ResourceKey<T> keyToRegister,
            T thingToRegister
    ) {
        Registry<T> reg = server.registryAccess().registryOrThrow(registry);
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.unfreeze();
            mapped.register(keyToRegister, thingToRegister, Lifecycle.stable());
            mapped.freeze();
        } else {
            throw new IllegalArgumentException("Cannot modify registry: " + registry.registry());
        }
    }
}
