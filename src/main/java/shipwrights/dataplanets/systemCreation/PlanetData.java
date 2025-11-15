package shipwrights.dataplanets.systemCreation;

import net.minecraft.resources.ResourceLocation;
import shipwrights.dataplanets.systemCreation.dimension.blocks.BlockInfo;
import shipwrights.dataplanets.systemCreation.dimension.blocks.BlockPalettes;
import shipwrights.dataplanets.util.Color;

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
        double gravity,
        Color color
) {

    public static PlanetData fromPlanetSource(PlanetSource source) {
        double temperature = deriveTemperature(source.distanceFromStar(), source.atmosphericDensity());
        double gravity = deriveGravity(source.size());
        double orbitalPeriod = deriveOrbitalPeriod(source.distanceFromStar());
        double effectiveHumidity = Math.min(1.0, source.seaLevel() * source.atmosphericDensity());

        Optional<BlockInfo> primaryBlockInfo = derivePrimaryBlockInfo(
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

        Color color = primaryBlockInfo
                .map(BlockInfo::getColor)
                .orElse(new Color(128, 128, 128, 255));

        return new PlanetData(
            source.name(),
            source.size(),
            source.distanceFromStar(),
            source.atmosphericDensity(),
            orbitalPeriod,
            source.weirdness(),
            source.seaLevel(),
            temperature,
            source.terrainRoughness(),
            source.flavour(),
            primaryBlockInfo.map(BlockInfo::block).orElse(ResourceLocation.parse("minecraft:stone")),
            primaryFluid,
            gravity,
            color
        );
    }

    private static Optional<BlockInfo> derivePrimaryBlockInfo(double mass, double temperature, double humidity, double weirdness) {
        if (BlockPalettes.SOLIDS.isEmpty()) {
            throw new IllegalStateException("No block palettes to choose from");
        }

        // Find the best matching block from BlockPalettes using all available parameters
        // Weight each parameter by its importance in block selection
        return BlockPalettes.SOLIDS.stream()
                .min(Comparator.comparingDouble(block ->
                    Math.abs(block.mass() - mass) * 0.5 +
                    Math.abs(block.temperature() - temperature) * 1.5 +
                    Math.abs(block.humidity() - humidity) * 0.8 +
                    Math.abs(block.weirdness() - weirdness) * 1.2
                ));
    }

    private static ResourceLocation derivePrimaryFluid(double mass, double temperature, double humidity, double weirdness) {
        if (BlockPalettes.FLUIDS.isEmpty()) {
            throw new IllegalStateException("No fluid palettes to choose from");
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

    private static double deriveOrbitalPeriod(double distanceFromStar) {
        // Kepler's third law: T² ∝ a³, so T ∝ a^1.5
        return Math.pow(distanceFromStar, 1.5);
    }

    public Color getPrimaryColour() {
        return color;
    }
}
