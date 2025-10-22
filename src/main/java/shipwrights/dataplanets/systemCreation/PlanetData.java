package shipwrights.dataplanets.systemCreation;

import net.minecraft.resources.ResourceLocation;

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
        ResourceLocation primaryBlock = derivePrimaryBlock(temperature, source.weirdness());
        ResourceLocation primaryFluid = derivePrimaryFluid(temperature, source.seaLevel(), source.atmosphericDensity());
        double gravity = deriveGravity(source.size());

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

    private static ResourceLocation derivePrimaryBlock(double temperature, double weirdness) {
        // Derive primary block based on temperature and weirdness
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

    private static ResourceLocation derivePrimaryFluid(double temperature, double seaLevel, double atmosphericDensity) {

        if (seaLevel < 0.3 || atmosphericDensity < 0.3) {
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
