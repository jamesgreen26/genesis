package shipwrights.dataplanets.systemCreation.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

/**
 * Creates NoiseGeneratorSettings for procedural terrain generation based on planet data.
 * Follows the pattern established in DimensionTypeCreator and BiomeCreator.
 */
public class TerrainGenCreator {

    /**
     * Create noise generator settings from planet data
     * @param planetData The planet data to create terrain generation settings from
     * @return NoiseGeneratorSettings configured for this planet
     */
    public static Holder.Reference<NoiseGeneratorSettings> createFromPlanetData(PlanetData planetData, SystemCreator.SystemCreationContext context) {
        // Convert planet's sea level (0.0-2.0 normalized) to Minecraft Y coordinate
        // Earth-like sea level (~1.0) should map to around Y=63
        int seaLevel = (int) (planetData.seaLevel() * 63);

        // Get block states from planet data
        BlockState defaultBlock = getBlockState(planetData.primaryBlock());
        BlockState defaultFluid = getBlockState(planetData.primaryFluid());

        // Create noise settings based on planet size
        // Larger planets get more vertical space
        NoiseSettings noiseSettings = createNoiseSettings(planetData);

        // Create noise router for terrain shape
        NoiseRouter noiseRouter = createNoiseRouter(planetData, context);

        // Create surface rules for block placement
        SurfaceRules.RuleSource surfaceRule = createSurfaceRules(planetData);

        // Use overworld spawn targets (can be customized later)
        List<Climate.ParameterPoint> spawnTarget = new OverworldBiomeBuilder().spawnTarget();

        // Determine generation flags
        boolean disableMobGeneration = false; // Allow mobs for now
        boolean aquifersEnabled = planetData.seaLevel() > 0.3; // Only if planet has significant water
        boolean oreVeinsEnabled = true; // Enable ore veins
        boolean useLegacyRandomSource = false; // Use modern random

        NoiseGeneratorSettings noiseGeneratorSettings =  new NoiseGeneratorSettings(
                noiseSettings,
                defaultBlock,
                defaultFluid,
                noiseRouter,
                surfaceRule,
                spawnTarget,
                seaLevel,
                disableMobGeneration,
                aquifersEnabled,
                oreVeinsEnabled,
                useLegacyRandomSource
        );

        ResourceLocation noiseSettingsLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name() + "_noise_settings");
        ResourceKey<NoiseGeneratorSettings> noiseSettingsKey = ResourceKey.create(Registries.NOISE_SETTINGS, noiseSettingsLocation);

        RegistryUtil.registerNoiseSettings(context.server, noiseSettingsLocation, noiseGeneratorSettings);

        return context.server.registryAccess()
                .registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(noiseSettingsKey);
    }

    /**
     * Create noise settings based on planet characteristics
     */
    private static NoiseSettings createNoiseSettings(PlanetData planetData) {
        // Base dimensions
        int minY = -64;
        int height = 384;

        // Adjust based on planet size
        // Larger planets (size > 1.5) get more vertical space
        if (planetData.size() > 1.5) {
            height = 448; // Taller terrain
        } else if (planetData.size() < 0.5) {
            height = 256; // Shorter terrain
            minY = -32;
        }

        // Noise size affects terrain detail
        // More terrain roughness = smaller horizontal noise size (more detail)
        int horizontalSize = planetData.terrainRoughness() > 1.5 ? 1 : 2;
        int verticalSize = 2; // Standard vertical noise

        return NoiseSettings.create(minY, height, horizontalSize, verticalSize);
    }

    /**
     * Clamp a value to the valid climate parameter range [-2.0, 2.0]
     * Uses a slightly tighter bound to account for floating point precision
     */
    private static double clampClimateParameter(double value) {
        return Math.max(-1.99, Math.min(1.99, value));
    }

    /**
     * Create a noise router for terrain generation based on planet properties
     */
    private static NoiseRouter createNoiseRouter(PlanetData planetData, SystemCreator.SystemCreationContext context) {
        HolderLookup.RegistryLookup<NormalNoise.NoiseParameters> noiseRegistry =
                context.server.registryAccess().lookupOrThrow(Registries.NOISE);

        // Calculate noise parameters from planet data
        double noiseScale = Math.max(0.1, planetData.terrainRoughness());
        double noiseAmplitude = planetData.size() * 0.3; // Reduced from 0.8 for more traversable terrain
        double weirdnessFactor = Math.abs(planetData.weirdness());
        double seaLevelOffset = (planetData.seaLevel() - 1.0) * 16.0; // Reduced from 32.0

        // Clamp climate parameters to valid range [-2.0, 2.0]
        double temperature = clampClimateParameter(planetData.temperature());
        double vegetation = clampClimateParameter(planetData.atmosphericDensity());
        double erosion = clampClimateParameter(planetData.weirdness() * 0.8);
        double ridges = clampClimateParameter(planetData.terrainRoughness() * 0.8);

        // Cache commonly used noise holders for performance
        Holder<NormalNoise.NoiseParameters> shiftNoise = noiseRegistry.getOrThrow(Noises.SHIFT);
        Holder<NormalNoise.NoiseParameters> continentalnessNoise = noiseRegistry.getOrThrow(Noises.CONTINENTALNESS);

        // Pre-calculate shift functions for reuse
        DensityFunction shiftA = DensityFunctions.shiftA(shiftNoise);
        DensityFunction shiftB = DensityFunctions.shiftB(shiftNoise);

        // Base terrain noise - varies by planet type with reduced amplitudes
        DensityFunction baseTerrainNoise;
        if (weirdnessFactor > 1.5) {
            // Weird planets: moderate chaotic terrain (reduced from 1.5x to 0.9x)
            baseTerrainNoise = DensityFunctions.mul(
                    DensityFunctions.constant(noiseAmplitude * 0.9),
                    DensityFunctions.noise(
                            noiseRegistry.getOrThrow(Noises.CAVE_CHEESE),
                            noiseScale * 1.2,
                            noiseScale * 0.6
                    )
            );
        } else if (planetData.terrainRoughness() > 1.3) {
            // Rough planets: moderate mountains (reduced from 1.2x to 0.75x)
            baseTerrainNoise = DensityFunctions.mul(
                    DensityFunctions.constant(noiseAmplitude * 0.75),
                    DensityFunctions.noise(
                            noiseRegistry.getOrThrow(Noises.JAGGED),
                            noiseScale,
                            noiseScale * 0.5
                    )
            );
        } else if (planetData.terrainRoughness() < 0.7) {
            // Smooth planets: very gentle hills (reduced from 0.6x to 0.4x)
            baseTerrainNoise = DensityFunctions.mul(
                    DensityFunctions.constant(noiseAmplitude * 0.4),
                    DensityFunctions.noise(
                            continentalnessNoise,
                            noiseScale * 0.4,
                            noiseScale * 0.2
                    )
            );
        } else {
            // Normal planets: moderate terrain (reduced from 1.0x to 0.6x)
            baseTerrainNoise = DensityFunctions.mul(
                    DensityFunctions.constant(noiseAmplitude * 0.6),
                    DensityFunctions.noise(
                            noiseRegistry.getOrThrow(Noises.RIDGE),
                            noiseScale * 0.8,
                            noiseScale * 0.4
                    )
            );
        }

        // Simplified detail layer - only add if flavour is significant (performance optimization)
        DensityFunction detailNoise = Math.abs(planetData.flavour()) > 0.5
                ? DensityFunctions.mul(
                        DensityFunctions.constant(noiseAmplitude * 0.15), // Reduced from 0.3
                        DensityFunctions.noise(
                                noiseRegistry.getOrThrow(Noises.GRAVEL),
                                noiseScale * 2.0,
                                noiseScale
                        )
                )
                : DensityFunctions.zero(); // Use zero() instead of constant(0) for performance

        // Simplified continents - reduced amplitude and using cached noise
        DensityFunction continents = DensityFunctions.mul(
                DensityFunctions.constant(planetData.size() * 0.2), // Reduced from 0.4
                DensityFunctions.noise(continentalnessNoise, 0.08, 0.04)
        );

        // Simplified depth - much less vertical variation
        DensityFunction depth = DensityFunctions.mul(
                DensityFunctions.constant(erosion * 0.25), // Reduced from 0.5
                DensityFunctions.noise(
                        noiseRegistry.getOrThrow(Noises.EROSION),
                        0.15,
                        0.08
                )
        );

        // Combine terrain elements more efficiently with fewer nested operations
        DensityFunction combinedTerrain = DensityFunctions.add(
                baseTerrainNoise,
                DensityFunctions.add(detailNoise, continents)
        );

        DensityFunction finalDensity = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1.0, -1.0),
                DensityFunctions.add(
                        combinedTerrain,
                        DensityFunctions.add(depth, DensityFunctions.constant(seaLevelOffset * 0.008))
                )
        );

        // Cache noise holders for climate parameters
        Holder<NormalNoise.NoiseParameters> temperatureNoise = noiseRegistry.getOrThrow(
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "temperature")));
        Holder<NormalNoise.NoiseParameters> vegetationNoise = noiseRegistry.getOrThrow(
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "vegetation")));
        Holder<NormalNoise.NoiseParameters> erosionNoise = noiseRegistry.getOrThrow(
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "erosion")));
        Holder<NormalNoise.NoiseParameters> ridgeNoise = noiseRegistry.getOrThrow(
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "ridge")));

        // Aquifer configuration based on planet properties - optimize with constants when disabled
        DensityFunction aquiferFloodedness = planetData.seaLevel() > 0.5
                ? DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 1.0, 0.0)
                : DensityFunctions.zero();

        DensityFunction aquiferSpread = planetData.seaLevel() > 0.5
                ? DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 1.0, 0.0)
                : DensityFunctions.zero();

        // Lava presence based on temperature
        DensityFunction lavaNoise = planetData.temperature() > 1.5
                ? DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.AQUIFER_LAVA), 1.0, 0.0)
                : DensityFunctions.constant(-1.0);

        // Build noise router with enhanced climate-based noise functions using cached values
        return new NoiseRouter(
                // barrierNoise - used for world border effects
                DensityFunctions.zero(),
                // fluidLevelFloodednessNoise - affects aquifer flooding
                aquiferFloodedness,
                // fluidLevelSpreadNoise - affects aquifer spread
                aquiferSpread,
                // lavaNoise - determines lava placement in aquifers
                lavaNoise,
                // temperature - biome climate parameter (scaled by planet temperature, using cached shift)
                DensityFunctions.mul(
                        DensityFunctions.constant(temperature),
                        DensityFunctions.shiftedNoise2d(shiftA, shiftB, 0.25F, temperatureNoise)
                ),
                // vegetation - biome climate parameter (scaled by atmospheric density, using cached shift)
                DensityFunctions.mul(
                        DensityFunctions.constant(vegetation),
                        DensityFunctions.shiftedNoise2d(shiftA, shiftB, 0.25F, vegetationNoise)
                ),
                // continents - large scale terrain variation
                continents,
                // erosion - terrain weathering (scaled by weirdness for variety, using cached shift)
                DensityFunctions.mul(
                        DensityFunctions.constant(erosion),
                        DensityFunctions.shiftedNoise2d(shiftA, shiftB, 0.25F, erosionNoise)
                ),
                // depth - vertical terrain variation
                depth,
                // ridges - mountain ridge generation (scaled by terrain roughness, using cached shift)
                DensityFunctions.mul(
                        DensityFunctions.constant(ridges),
                        DensityFunctions.shiftedNoise2d(shiftA, shiftB, 0.25F, ridgeNoise)
                ),
                // initialDensityWithoutJaggedness - base terrain density
                baseTerrainNoise,
                // finalDensity - determines block vs air placement
                finalDensity,
                // veinToggle - ore vein generation toggle
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.ORE_VEININESS), 1.0, 1.0),
                // veinRidged - ore vein ridged noise
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.ORE_VEIN_A), 1.0, 1.0),
                // veinGap - ore vein gap noise
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.ORE_GAP), 1.0, 1.0)
        );
    }

    /**
     * Create surface rules based on planet properties
     */
    private static SurfaceRules.RuleSource createSurfaceRules(PlanetData planetData) {
        BlockState primaryBlock = getBlockState(planetData.primaryBlock());

        // Create basic surface rule: primary block at surface
        SurfaceRules.RuleSource primarySurface = SurfaceRules.state(primaryBlock);

        // Add surface variation based on temperature and weirdness
        if (planetData.temperature() > 1.5) {
            // Hot planets - add some variety with sand/red sandstone
            return SurfaceRules.sequence(
                    SurfaceRules.ifTrue(
                            SurfaceRules.ON_FLOOR,
                            SurfaceRules.state(Blocks.RED_SAND.defaultBlockState())
                    ),
                    primarySurface
            );
        } else if (planetData.temperature() < 0.5) {
            // Cold planets - add snow/ice layers
            return SurfaceRules.sequence(
                    SurfaceRules.ifTrue(
                            SurfaceRules.ON_FLOOR,
                            SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState())
                    ),
                    primarySurface
            );
        } else if (planetData.weirdness() > 1.5) {
            // Weird planets - unusual surface
            return SurfaceRules.sequence(
                    SurfaceRules.ifTrue(
                            SurfaceRules.ON_FLOOR,
                            SurfaceRules.state(Blocks.MOSS_BLOCK.defaultBlockState())
                    ),
                    primarySurface
            );
        }

        // Default: just use primary block
        return primarySurface;
    }

    /**
     * Helper to get BlockState from ResourceLocation
     */
    private static BlockState getBlockState(net.minecraft.resources.ResourceLocation resourceLocation) {
        return BuiltInRegistries.BLOCK.get(resourceLocation).defaultBlockState();
    }
}
