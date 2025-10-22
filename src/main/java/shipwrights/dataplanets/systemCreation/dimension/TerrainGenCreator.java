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
        // terrainRoughness affects the scale of noise (higher = more detail)
        double noiseScale = planetData.terrainRoughness();
        double noiseAmplitude = planetData.size(); // Larger planets = more dramatic terrain

        // Clamp climate parameters to valid range [-2.0, 2.0]
        double temperature = clampClimateParameter(planetData.temperature());
        double vegetation = clampClimateParameter(planetData.atmosphericDensity());
        double erosion = clampClimateParameter(planetData.weirdness());
        double ridges = clampClimateParameter(planetData.terrainRoughness());

        // Create final density function for terrain shape
        // This determines where blocks are placed vs air
        DensityFunction finalDensity = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1.0, -1.0),
                DensityFunctions.mul(
                        DensityFunctions.constant(noiseAmplitude),
                        DensityFunctions.noise(
                                noiseRegistry.getOrThrow(Noises.GRAVEL),
                                noiseScale,
                                noiseScale * 0.5
                        )
                )
        );

        // Get noise holders for climate parameters
        ResourceKey<NormalNoise.NoiseParameters> temperatureNoiseKey =
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "temperature"));
        ResourceKey<NormalNoise.NoiseParameters> vegetationNoiseKey =
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "vegetation"));
        ResourceKey<NormalNoise.NoiseParameters> erosionNoiseKey =
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "erosion"));
        ResourceKey<NormalNoise.NoiseParameters> ridgeNoiseKey =
                ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath("minecraft", "ridge"));

        // Build noise router with climate-based noise functions
        return new NoiseRouter(
                // barrierNoise - used for world border effects
                DensityFunctions.constant(0),
                // fluidLevelFloodednessNoise - affects aquifer flooding
                DensityFunctions.constant(0),
                // fluidLevelSpreadNoise - affects aquifer spread
                DensityFunctions.constant(0),
                // lavaNoise - determines lava placement in aquifers
                DensityFunctions.constant(0),
                // temperature - biome climate parameter (scaled by planet temperature)
                DensityFunctions.mul(
                        DensityFunctions.constant(temperature),
                        DensityFunctions.shiftedNoise2d(
                                DensityFunctions.shiftA(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                DensityFunctions.shiftB(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                0.25F,
                                noiseRegistry.getOrThrow(temperatureNoiseKey)
                        )
                ),
                // vegetation - biome climate parameter (scaled by atmospheric density)
                DensityFunctions.mul(
                        DensityFunctions.constant(vegetation),
                        DensityFunctions.shiftedNoise2d(
                                DensityFunctions.shiftA(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                DensityFunctions.shiftB(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                0.25F,
                                noiseRegistry.getOrThrow(vegetationNoiseKey)
                        )
                ),
                // continents - large scale terrain variation
                DensityFunctions.constant(0),
                // erosion - terrain weathering (scaled by weirdness for variety)
                DensityFunctions.mul(
                        DensityFunctions.constant(erosion),
                        DensityFunctions.shiftedNoise2d(
                                DensityFunctions.shiftA(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                DensityFunctions.shiftB(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                0.25F,
                                noiseRegistry.getOrThrow(erosionNoiseKey)
                        )
                ),
                // depth - vertical terrain variation
                DensityFunctions.constant(0),
                // ridges - mountain ridge generation (scaled by terrain roughness)
                DensityFunctions.mul(
                        DensityFunctions.constant(ridges),
                        DensityFunctions.shiftedNoise2d(
                                DensityFunctions.shiftA(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                DensityFunctions.shiftB(noiseRegistry.getOrThrow(Noises.SHIFT)),
                                0.25F,
                                noiseRegistry.getOrThrow(ridgeNoiseKey)
                        )
                ),
                // initialDensityWithoutJaggedness - base terrain density
                DensityFunctions.constant(0),
                // finalDensity - determines block vs air placement
                finalDensity,
                // veinToggle - ore vein generation toggle
                DensityFunctions.constant(0),
                // veinRidged - ore vein ridged noise
                DensityFunctions.constant(0),
                // veinGap - ore vein gap noise
                DensityFunctions.constant(0)
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
