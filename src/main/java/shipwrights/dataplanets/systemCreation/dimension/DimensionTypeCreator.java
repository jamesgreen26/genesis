package shipwrights.dataplanets.systemCreation.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.fml.ModList;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.OptionalLong;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

/**
 * Builtin dimension types for different planet configurations
 * Based on combinations of atmosphere and oxygen presence
 */
public class DimensionTypeCreator {

    public static Holder<DimensionType> createAndRegisterDimensionType(SystemCreator.SystemCreationContext context, PlanetData planetData) {
        DimensionType dimensionType = DimensionTypeCreator.createFromPlanetData(planetData);
        ResourceLocation dimensionTypeLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name() + "_dimension_type");
        ResourceKey<DimensionType> dimensionTypeKey = ResourceKey.create(Registries.DIMENSION_TYPE, dimensionTypeLocation);

        RegistryUtil.registerDimensionType(
                context.server,
                dimensionTypeLocation,
                dimensionType
        );

        // Return holder from registry after registration
        return context.server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(dimensionTypeKey);
    }

    /**
     * Create a dimension type based on planet data
     * @param planetData The planet data to create a dimension type from
     * @return The appropriate dimension type
     */
    private static DimensionType createFromPlanetData(PlanetData planetData) {

        // Closer to star = more skylight (inverse relationship)
        // Earth distance is ~1.0, so less than ~3.0 has good skylight
        boolean hasSkylight = planetData.distanceFromStar() < 3.0;

        // Temperature thresholds (using normalized scale where Earth = 1.0)
        boolean ultrawarm = planetData.temperature() > 1.8;  // Very hot planets
        boolean monsterSpawn = planetData.temperature() > 0.5 && planetData.temperature() < 2.0;  // Habitable temperature range

        // Atmosphere threshold - anything above 0.3 is considered "has atmosphere"
        boolean hasAtmosphere = planetData.atmosphericDensity() > 0.3;

        if (hasAtmosphere) {
            // Planet with atmosphere - overworld effects
            return createWithAtmosphere(hasSkylight, ultrawarm, false, monsterSpawn);
        } else {
            // Airless planet - end effects
            return createAirless(hasSkylight, ultrawarm, monsterSpawn);
        }
    }

    /**
     * Planets with atmosphere
     * Uses overworld effects (sky, clouds, weather)
     */
    public static DimensionType createWithAtmosphere(boolean hasSkylight, boolean ultrawarm, boolean hasCeiling, boolean monsterSpawn) {
        return new DimensionType(
            OptionalLong.empty(),                    // fixed_time - none (day/night cycle)
            hasSkylight,                             // has_skylight
            hasCeiling,                              // has_ceiling
            ultrawarm,                               // ultrawarm (affects water evaporation, etc)
            true,                                    // natural (compass/clock work)
            1.0,                                     // coordinate_scale
            true,                                    // bed_works
            false,                                   // respawn_anchor_works
            -64,                                     // min_y
            384,                                     // height
            384,                                     // logical_height
            BlockTags.INFINIBURN_OVERWORLD,          // infiniburn
            ResourceLocation.withDefaultNamespace("overworld"), // effects
            0.0f,                                    // ambient_light
            new DimensionType.MonsterSettings(monsterSpawn, false, UniformInt.of(0, 7), 0)
        );
    }

    /**
     * Airless/vacuum planets (no atmosphere)
     * Uses end effects (no sky, void particles, different ambient)
     */
    public static DimensionType createAirless(boolean hasSkylight, boolean ultrawarm, boolean monsterSpawn) {
        return new DimensionType(
            OptionalLong.empty(),                    // fixed_time
            hasSkylight,                             // has_skylight
            false,                                   // has_ceiling
            ultrawarm,                               // ultrawarm
            true,                                    // natural
            1.0,                                     // coordinate_scale
            false,                                   // bed_works (beds explode in airless)
            true,                                    // respawn_anchor_works
            -64,                                     // min_y
            384,                                     // height
            384,                                     // logical_height
            BlockTags.INFINIBURN_OVERWORLD,          // infiniburn
            getSpaceEffects(), // effects
            0.0f,                                    // ambient_light
            new DimensionType.MonsterSettings(monsterSpawn, false, UniformInt.of(0, 7), 0)
        );
    }

    private static ResourceLocation getSpaceEffects() {
        boolean isGenesLoaded = ModList.get().isLoaded("genesis");

        if (isGenesLoaded) {
            return ResourceLocation.parse("genesis:great_unknown");
        } else {
            return ResourceLocation.parse("minecraft:the_end");
        }
    }
}
