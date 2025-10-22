package shipwrights.dataplanets.systemCreation.dimension;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import shipwrights.dataplanets.systemCreation.PlanetData;

public class BiomeCreator {

    public static Biome createBiome(RandomSource random, PlanetData planetData, double variationFactor) {
        // Vary temperature based on planet base temperature and variation
        float temperature = (float) (planetData.temperature() + (variationFactor - 0.5) * 0.4);

        // Vary downfall (precipitation) based on atmospheric density and sea level
        float downfall = (float) ((planetData.atmosphericDensity() + planetData.seaLevel()) / 2.0);

        // Determine if this biome has precipitation
        boolean hasPrecipitation = downfall > 0.3 && temperature > 0.15;

        // Create special effects based on planet properties
        BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
                .fogColor(planetData.fogColor())
                .waterColor(deriveWaterColor(planetData, variationFactor))
                .waterFogColor(deriveWaterFogColor(planetData, variationFactor))
                .skyColor(deriveSkyColor(planetData, variationFactor))
                .grassColorOverride(deriveGrassColor(planetData, variationFactor))
                .foliageColorOverride(deriveFoliageColor(planetData, variationFactor));

        // Create mob spawn settings (empty for now - no mobs on generated planets)
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();

        // Create generation settings (empty for now - will be filled in later)
        BiomeGenerationSettings generationSettings = new BiomeGenerationSettings.PlainBuilder().build();

        // Build and return the biome
        return new Biome.BiomeBuilder()
                .hasPrecipitation(hasPrecipitation)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(effectsBuilder.build())
                .mobSpawnSettings(mobSpawnSettings)
                .generationSettings(generationSettings)
                .build();
    }

    private static int deriveWaterColor(PlanetData planetData, double variationFactor) {
        // Base water color on primary fluid and temperature
        if (planetData.temperature() > 1.8) {
            // Lava planets - orange/red water
            return 0xFF6600 + (int)(variationFactor * 0x004400);
        } else if (planetData.temperature() < 0.4) {
            // Ice planets - light blue/white water
            return 0xAADDFF - (int)(variationFactor * 0x002200);
        } else {
            // Normal water - vary from deep blue to cyan
            return 0x3F76E4 + (int)(variationFactor * 0x204020);
        }
    }

    private static int deriveWaterFogColor(PlanetData planetData, double variationFactor) {
        // Water fog is generally darker than water color
        int waterColor = deriveWaterColor(planetData, variationFactor);
        return (waterColor & 0xFEFEFE) >> 1; // Darken by dividing RGB components by 2
    }

    private static int deriveSkyColor(PlanetData planetData, double variationFactor) {
        double temp = planetData.temperature();
        double atmosphere = planetData.atmosphericDensity();

        if (atmosphere < 0.3) {
            // Thin atmosphere - dark/black sky
            return (int)(variationFactor * 0x101010);
        } else if (temp > 2.0) {
            // Very hot - reddish sky
            return 0x8B0000 + (int)(variationFactor * 0x300000);
        } else if (temp > 1.5) {
            // Hot - orange sky
            return 0xFF8C00 - (int)(variationFactor * 0x002000);
        } else if (temp < 0.5) {
            // Cold - pale sky
            return 0xC0D8FF - (int)(variationFactor * 0x001020);
        } else {
            // Earth-like - blue sky
            return 0x78A7FF + (int)(variationFactor * 0x040400);
        }
    }

    private static int deriveGrassColor(PlanetData planetData, double variationFactor) {
        double temp = planetData.temperature();
        double weirdness = planetData.weirdness();

        if (weirdness > 1.5) {
            // Weird planets - purple/pink vegetation
            return 0xAA55AA + (int)(variationFactor * 0x003300);
        } else if (temp > 1.5) {
            // Hot planets - yellow/brown grass
            return 0xBFB755 - (int)(variationFactor * 0x202000);
        } else if (temp < 0.5) {
            // Cold planets - blue-green grass
            return 0x5599AA + (int)(variationFactor * 0x002200);
        } else {
            // Normal - green grass
            return 0x77AB2F + (int)(variationFactor * 0x102010);
        }
    }

    private static int deriveFoliageColor(PlanetData planetData, double variationFactor) {
        // Foliage is generally similar to grass but slightly different
        int grassColor = deriveGrassColor(planetData, variationFactor);
        // Shift the color slightly towards darker/more saturated
        return (grassColor & 0xFEFEFE) - 0x101010;
    }
}
