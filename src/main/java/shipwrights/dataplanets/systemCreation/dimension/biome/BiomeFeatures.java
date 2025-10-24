package shipwrights.dataplanets.systemCreation.dimension.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;

import java.util.Optional;

public class BiomeFeatures {

    public static @NotNull BiomeGenerationSettings getBiomeGenerationSettings(SystemCreator.SystemCreationContext context, PlanetData planetData, double variationFactor) {
        BiomeGenerationSettings.PlainBuilder builder = new BiomeGenerationSettings.PlainBuilder();

        addCarvers(context, builder);

        if ((planetData.atmosphericDensity() > 0.8 || planetData.weirdness() > 0.8) && planetData.flavour() > 0.3) {
            addDripstone(context, builder);
        }

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
}
