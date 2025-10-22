package shipwrights.dataplanets;

import net.minecraft.util.RandomSource;

public record PlanetSource(
        double size,
        double distanceFromStar,
        double atmosphericDensity,
        double orbitalPeriod,
        double weirdness,
        double seaLevel,
        double temperature,
        double terrainRoughness,
        double flavour
) {

    public static PlanetSource createRandom(RandomSource random) {
        return builder()
                .size(doubleBetween(random, 0.5, 3.0))
                .distanceFromStar(doubleBetween(random, 0.25, 4.0))
                .orbitalPeriod(doubleBetween(random, 0.5, 4.0))
                .atmosphericDensity(doubleBetween(random, 0.0, 3.5))
                .weirdness(doubleBetween(random, 0.0, 2.0))
                .seaLevel(doubleBetween(random, 0.0, 2.0))
                .terrainRoughness(doubleBetween(random, 0.0, 2.0))
                .flavour(doubleBetween(random, 0.0, 2.0))
                .build();
    }

    private static double doubleBetween(RandomSource random, double lower, double upper) {
        return random.nextInt((int)(lower * 100), (int)(upper * 100)) / 100.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double size = 1.0;
        private double distanceFromStar = 1.0;
        private double atmosphericDensity = 1.0;
        private double orbitalPeriod = 1.0;
        private double weirdness = 1.0;
        private double seaLevel = 1.0;
        private double terrainRoughness = 1.0;
        private double flavour = 1.0;

        private Builder() {}

        public Builder size(double size) {
            this.size = size;
            return this;
        }

        public Builder distanceFromStar(double distanceFromStar) {
            this.distanceFromStar = distanceFromStar;
            return this;
        }

        public Builder orbitalPeriod(double orbitalPeriod) {
            this.orbitalPeriod = orbitalPeriod;
            return this;
        }

        public Builder atmosphericDensity(double atmosphericDensity) {
            this.atmosphericDensity = atmosphericDensity;
            return this;
        }

        public Builder weirdness(double weirdness) {
            this.weirdness = weirdness;
            return this;
        }

        public Builder seaLevel(double seaLevel) {
            this.seaLevel = seaLevel;
            return this;
        }

        public Builder terrainRoughness(double terrainRoughness) {
            this.terrainRoughness = terrainRoughness;
            return this;
        }

        public Builder flavour(double flavour) {
            this.flavour = flavour;
            return this;
        }

        public PlanetSource build() {
            return new PlanetSource(size, distanceFromStar, atmosphericDensity, orbitalPeriod, weirdness, seaLevel, getTemperature(), terrainRoughness, flavour);
        }

        private double getTemperature() {
            double baseTemp = 1.0 / (distanceFromStar * distanceFromStar);
            double greenhouseEffect = 1.0 + (atmosphericDensity - 1.0) * 0.3;
            return baseTemp * greenhouseEffect;
        }
    }
}
