package shipwrights.dataplanets.systemCreation.dimension.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BiomeFeatures {

    public static @NotNull BiomeGenerationSettings getBiomeGenerationSettings(SystemCreator.SystemCreationContext context, PlanetData planetData, double variationFactor, String biomeName) {
        BiomeGenerationSettings.PlainBuilder builder = new BiomeGenerationSettings.PlainBuilder();

        addCarvers(context, builder);

        if ((planetData.atmosphericDensity() > 0.8 || planetData.weirdness() > 0.8) && planetData.flavour() > 0.3) {
            addDripstone(context, builder);
        }

        addDeltas(context, builder, biomeName);

        return builder.build();
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

    private static void addDeltas(SystemCreator.SystemCreationContext context, BiomeGenerationSettings.PlainBuilder builder, String biomeName) {
        Registry<ConfiguredFeature<?,?>> configuredFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = context.server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        ResourceLocation configuredResourceLocation = ResourceLocation.tryBuild("dataplanets",biomeName+"_delta");
        ResourceLocation placedResourceLocation = ResourceLocation.tryBuild("dataplanets",biomeName+"_delta");

        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeatureRegistry.key(), configuredResourceLocation);
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(), placedResourceLocation);

        DeltaFeatureConfiguration configuration = new DeltaFeatureConfiguration(Blocks.LAVA.defaultBlockState(),Blocks.MAGMA_BLOCK.defaultBlockState(), UniformInt.of(3,7),UniformInt.of(0,2));
        ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.DELTA_FEATURE,configuration);

        RegistryUtil.registerConfiguredFeature(context.server, configuredResourceLocation, feature);

        List<PlacementModifier> modifiers = new ArrayList<>();
        modifiers.add(CountOnEveryLayerPlacement.of(40));
        PlacedFeature placedFeature = new PlacedFeature(configuredFeatureRegistry.getHolderOrThrow(configuredKey),modifiers);

        RegistryUtil.registerPlacedFeature(context.server, placedResourceLocation, placedFeature);

        Optional<Holder.Reference<PlacedFeature>> placedHolder = placedFeatureRegistry.getHolder(placedKey);

        placedHolder.ifPresent(ref -> builder.addFeature(0, ref));
    }


}
