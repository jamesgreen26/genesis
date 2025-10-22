package shipwrights.dataplanets.systemCreation.dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;
import shipwrights.dataplanets.systemCreation.PlanetData;

import java.util.OptionalLong;

/**
 * Builtin dimension types for different planet configurations
 * Based on combinations of atmosphere and oxygen presence
 */
public class DimensionTypeCreator {

    /**
     * Create a dimension type based on planet data
     * @param planetData The planet data to create a dimension type from
     * @return The appropriate dimension type
     */
    public static DimensionType createFromPlanetData(PlanetData planetData) {

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
            ResourceLocation.withDefaultNamespace("the_end"), // effects
            0.0f,                                    // ambient_light
            new DimensionType.MonsterSettings(monsterSpawn, false, UniformInt.of(0, 7), 0)
        );
    }
}
