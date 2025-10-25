package shipwrights.dataplanets.systemCreation;

import net.minecraft.resources.ResourceLocation;
import shipwrights.dataplanets.systemCreation.dimension.blocks.BlockInfo;
import shipwrights.dataplanets.systemCreation.dimension.blocks.BlockPalettes;

import java.util.Comparator;
import java.util.Optional;

/**
 * The full properties of a planet, including both core properties and derived properties
 * <br> <br>
 * All properties use Earth = 1.0 as a reference point
 * <br><br>
 * Derived properties should be fully deterministic, using hash functions if randomness is desired
 **/
public record PlanetData(
        String name,
        double size,
        double distanceFromStar,
        double atmosphericDensity,
        double orbitalPeriod,
        double weirdness,
        double seaLevel,
        double temperature,
        double terrainRoughness,
        double flavour,
        ResourceLocation primaryBlock,
        ResourceLocation primaryFluid,
        double gravity
) {

    public static PlanetData fromPlanetSource(PlanetSource source) {
        double temperature = deriveTemperature(source.distanceFromStar(), source.atmosphericDensity());
        double gravity = deriveGravity(source.size());
        double effectiveHumidity = Math.min(1.0, source.seaLevel() * source.atmosphericDensity());

        ResourceLocation primaryBlock = derivePrimaryBlock(
            source.size(),
            temperature,
            effectiveHumidity,
            source.weirdness()
        );

        ResourceLocation primaryFluid = derivePrimaryFluid(
            source.size(),
            temperature,
            effectiveHumidity,
            source.weirdness()
        );

        return new PlanetData(
            source.name(),
            source.size(),
            source.distanceFromStar(),
            source.atmosphericDensity(),
            source.orbitalPeriod(),
            source.weirdness(),
            source.seaLevel(),
            temperature,
            source.terrainRoughness(),
            source.flavour(),
            primaryBlock,
            primaryFluid,
            gravity
        );
    }

    private static ResourceLocation derivePrimaryBlock(double mass, double temperature, double humidity, double weirdness) {
        // If BlockPalettes are empty, use default values
        if (BlockPalettes.SOLIDS.isEmpty()) {
            if (temperature > 2.0) {
                return ResourceLocation.parse("minecraft:netherrack");
            } else if (temperature > 1.5) {
                return ResourceLocation.parse("minecraft:red_sandstone");
            } else if (temperature < 0.3) {
                return ResourceLocation.parse("minecraft:packed_ice");
            } else if (weirdness > 1.5) {
                return ResourceLocation.parse("minecraft:end_stone");
            } else {
                return ResourceLocation.parse("minecraft:stone");
            }
        }

        // Find the best matching block from BlockPalettes using all available parameters
        // Weight each parameter by its importance in block selection
        Optional<BlockInfo> bestMatch = BlockPalettes.SOLIDS.stream()
                .min(Comparator.comparingDouble(block ->
                    Math.abs(block.mass() - mass) * 0.5 +
                    Math.abs(block.temperature() - temperature) * 1.5 +
                    Math.abs(block.humidity() - humidity) * 0.8 +
                    Math.abs(block.weirdness() - weirdness) * 1.2
                ));

        return bestMatch.map(BlockInfo::block).orElse(ResourceLocation.parse("minecraft:stone"));
    }

    private static ResourceLocation derivePrimaryFluid(double mass, double temperature, double humidity, double weirdness) {
        // If BlockPalettes are empty, use default values
        if (BlockPalettes.FLUIDS.isEmpty()) {
            if (humidity < 0.3) {
                return ResourceLocation.parse("minecraft:air");
            }

            if (temperature < 0.4) {
                return ResourceLocation.parse("minecraft:ice");
            }

            if (temperature > 1.8) {
                return ResourceLocation.parse("minecraft:lava");
            }

            return ResourceLocation.parse("minecraft:water");
        }

        // Find the best matching fluid from BlockPalettes using all available parameters
        // Weight each parameter by its importance in fluid selection
        Optional<BlockInfo> bestMatch = BlockPalettes.FLUIDS.stream()
                .min(Comparator.comparingDouble(fluid ->
                    Math.abs(fluid.mass() - mass) * 0.9 +
                    Math.abs(fluid.temperature() - temperature) * 1.5 +
                    Math.abs(fluid.humidity() - humidity) * 1.2 +
                    Math.abs(fluid.weirdness() - weirdness) * 0.5
                ));

        return bestMatch.map(BlockInfo::block).orElse(ResourceLocation.parse("minecraft:water"));
    }

    private static double deriveGravity(double size) {
        // Gravity is primarily based on planet size (mass)
        return Math.sqrt(size);
    }

    private static double deriveTemperature(double distanceFromStar, double atmosphericDensity) {
        double baseTemp = 1.0 / distanceFromStar;
        double greenhouseEffect = 1.0 + (atmosphericDensity - 1.0) * 0.3;
        double rawTemp = baseTemp * greenhouseEffect;

        return Math.max(-2.0, Math.min(2.0, rawTemp));
    }
}
