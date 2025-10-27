package shipwrights.dataplanets.systemCreation.dimension.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.systemCreation.dimension.biome.features.CrystalFeature;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BiomeFeatures {

    public static @NotNull BiomeGenerationSettings getBiomeGenerationSettings(SystemCreator.SystemCreationContext context, PlanetData planetData, double variationFactor, String biomeName) {
        BiomeGenerationSettings.PlainBuilder builder = new BiomeGenerationSettings.PlainBuilder();

        RandomSource random = RandomSource.create(biomeName.hashCode());

        addCarvers(context, builder);

        if ((planetData.atmosphericDensity() > 0.8 || planetData.weirdness() > 0.8) && planetData.flavour() > 0.3) {
            addDripstone(context, builder);
        }

        if (planetData.atmosphericDensity() > 0.3 && random.nextDouble() > 0.7) {
            addDeltas(context, builder, biomeName, planetData);
        }

        if (planetData.atmosphericDensity() > 0.7) {
            addLakes(context, builder, biomeName, planetData);
        }

        if (planetData.temperature() < 0.5 && random.nextDouble() > 0.7) {
            addIceSpikes(context, builder);
        } else if (planetData.gravity() < 1 && random.nextDouble() > 0.8) {
            addCrystals(context, builder, biomeName, planetData);
        }

        return builder.build();
    }

    private static void addCrystals(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder, String biomeName, PlanetData planetData) {
        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(CountOnEveryLayerPlacement.of(40));

        addSimpleFeature(context, builder, biomeName + "_crystal", "crystal", modifiers, 0);
    }

    private static void addSimpleFeature(
            SystemCreator.SystemCreationContext context,
            BiomeGenerationSettings.PlainBuilder builder,
            String placedFeatureName,
            String featureName,
            List<PlacementModifier> modifiers,
            int decorationStep
    ) {
        Registry<ConfiguredFeature<?,?>> configuredFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Registry<Feature<?>> featureRegistry = context.server.registryAccess().registryOrThrow(Registries.FEATURE);

        ResourceLocation placedResourceLocation = ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, placedFeatureName);
        ResourceLocation featureResourceLocation = ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, featureName);

        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeatureRegistry.key(), placedResourceLocation);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(), placedResourceLocation);

        Holder<Feature<?>> feature = featureRegistry.getHolderOrThrow(ResourceKey.create(featureRegistry.key(), featureResourceLocation));

        @SuppressWarnings("unchecked")
        ConfiguredFeature<?, ?> configuredFeature = new ConfiguredFeature<>((Feature<NoneFeatureConfiguration>) feature.value(), FeatureConfiguration.NONE);

        RegistryUtil.registerConfiguredFeature(context.server, placedResourceLocation, configuredFeature);

        PlacedFeature placedFeature = new PlacedFeature(configuredFeatureRegistry.getHolderOrThrow(configuredKey), modifiers);

        RegistryUtil.registerPlacedFeature(context.server, placedResourceLocation, placedFeature);

        Optional<Holder.Reference<PlacedFeature>> placedHolder = placedFeatureRegistry.getHolder(placedKey);

        placedHolder.ifPresent(ref -> builder.addFeature(decorationStep, ref));
    }

    private static void addIceSpikes(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder) {
        Registry<PlacedFeature> placedFeatures = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        Optional<Holder.Reference<PlacedFeature>> iceSpikes = placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse("ice_spike")));

        iceSpikes.ifPresent(ref -> builder.addFeature(0, ref));
    }

    private static void addDripstone(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder) {
        Registry<PlacedFeature> placedFeatures = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        Optional<Holder.Reference<PlacedFeature>> dripstone = placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse("large_dripstone")));
        Optional<Holder.Reference<PlacedFeature>> dripstone_cluster = placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.parse("dripstone_cluster")));
        Optional<Holder.Reference<PlacedFeature>> pointed_dripstone = placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.parse("pointed_dripstone")));

        dripstone.ifPresent(ref -> builder.addFeature(0, ref));
        dripstone_cluster.ifPresent(ref -> builder.addFeature(0, ref));
        pointed_dripstone.ifPresent(ref -> builder.addFeature(0, ref));
    }

    private static void addCarvers(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder) {
        Registry<ConfiguredWorldCarver<?>> carvers = context.server.registryAccess().registryOrThrow(Registries.CONFIGURED_CARVER);

        Holder.Reference<ConfiguredWorldCarver<?>> canyon = carvers.getHolderOrThrow(Carvers.CANYON);
        Holder.Reference<ConfiguredWorldCarver<?>> cave = carvers.getHolderOrThrow(Carvers.CAVE);
        Holder.Reference<ConfiguredWorldCarver<?>> cave_extra = carvers.getHolderOrThrow(Carvers.CAVE_EXTRA_UNDERGROUND);

        builder.addCarver(GenerationStep.Carving.AIR,canyon)
                .addCarver(GenerationStep.Carving.AIR,cave)
                .addCarver(GenerationStep.Carving.AIR,cave_extra);
    }

    private static void addDeltas(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder, String biomeName, PlanetData planetData) {
        Registry<ConfiguredFeature<?,?>> configuredFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        ResourceLocation configuredResourceLocation = ResourceLocation.fromNamespaceAndPath("dataplanets",biomeName+"_delta");
        ResourceLocation placedResourceLocation = ResourceLocation.fromNamespaceAndPath("dataplanets",biomeName+"_delta");

        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeatureRegistry.key(), configuredResourceLocation);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(), placedResourceLocation);

        ConfiguredFeature<?, ?> feature = getConfiguredDelta(context.server, planetData);

        RegistryUtil.registerConfiguredFeature(context.server, configuredResourceLocation, feature);

        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(CountOnEveryLayerPlacement.of(40));
        PlacedFeature placedFeature = new PlacedFeature(configuredFeatureRegistry.getHolderOrThrow(configuredKey),modifiers);

        RegistryUtil.registerPlacedFeature(context.server, placedResourceLocation, placedFeature);

        Optional<Holder.Reference<PlacedFeature>> placedHolder = placedFeatureRegistry.getHolder(placedKey);

        placedHolder.ifPresent(ref -> builder.addFeature(0, ref));
    }

    private static @NotNull ConfiguredFeature<?, ?> getConfiguredDelta(MinecraftServer server, PlanetData planetData) {
        Block primaryFluid = server.registryAccess().registryOrThrow(Registries.BLOCK).get(planetData.primaryFluid());

        BlockState primaryFluidState = Objects.requireNonNullElse(primaryFluid, Blocks.LAVA).defaultBlockState();

        BlockState solidState;

        if (primaryFluidState == Blocks.LAVA.defaultBlockState()) {
            solidState = Blocks.MAGMA_BLOCK.defaultBlockState();
        } else {
            solidState = Blocks.MUD.defaultBlockState();
        }

        DeltaFeatureConfiguration configuration = new DeltaFeatureConfiguration(primaryFluidState, solidState, UniformInt.of(3,7),UniformInt.of(0,2));
        return new ConfiguredFeature<>(Feature.DELTA_FEATURE, configuration);
    }

    private static void addLakes(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder, String biomeName, PlanetData planetData) {
        Registry<ConfiguredFeature<?,?>> configuredFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        ResourceLocation configuredResourceLocation = ResourceLocation.fromNamespaceAndPath("dataplanets",biomeName+"_lake");
        ResourceLocation placedResourceLocation = ResourceLocation.fromNamespaceAndPath("dataplanets",biomeName+"_lake");

        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeatureRegistry.key(), configuredResourceLocation);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(), placedResourceLocation);

        ConfiguredFeature<?, ?> feature = getConfiguredLake(context.server, planetData);

        RegistryUtil.registerConfiguredFeature(context.server, configuredResourceLocation, feature);

        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(CountOnEveryLayerPlacement.of(1));
        PlacedFeature placedFeature = new PlacedFeature(configuredFeatureRegistry.getHolderOrThrow(configuredKey),modifiers);

        RegistryUtil.registerPlacedFeature(context.server, placedResourceLocation, placedFeature);

        Optional<Holder.Reference<PlacedFeature>> placedHolder = placedFeatureRegistry.getHolder(placedKey);

        placedHolder.ifPresent(ref -> builder.addFeature(0, ref));
    }

    private static @NotNull ConfiguredFeature<?, ?> getConfiguredLake(MinecraftServer server, PlanetData planetData) {
        Block primaryFluid = server.registryAccess().registryOrThrow(Registries.BLOCK).get(planetData.primaryFluid());

        BlockState primaryFluidState = Objects.requireNonNullElse(primaryFluid, Blocks.LAVA).defaultBlockState();

        BlockState solidState;

        if (primaryFluidState == Blocks.LAVA.defaultBlockState()) {
            solidState = Blocks.MAGMA_BLOCK.defaultBlockState();
        } else {
            solidState = BuiltInRegistries.BLOCK.get(planetData.primaryBlock()).defaultBlockState();
        }

        LakeFeature.Configuration configuration = new LakeFeature.Configuration(BlockStateProvider.simple(primaryFluidState), BlockStateProvider.simple(solidState));
        return new ConfiguredFeature<>(Feature.LAKE, configuration);
    }


}
