package shipwrights.dataplanets.systemCreation;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.naming.SystemNameGenerator;
import shipwrights.dataplanets.systemCreation.dimension.BiomeCreator;
import shipwrights.dataplanets.systemCreation.dimension.DimensionTypeCreator;
import shipwrights.dataplanets.systemCreation.dimension.TerrainGenCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

public class SystemCreator {

    public void createSystem(MinecraftServer server, boolean scientificNamingStyle) {
        SystemCreationContext context = new SystemCreationContext(server, scientificNamingStyle);

        int planetCount = context.random.nextInt(4, 9);
        for (int i = 0; i < planetCount; i++) {
            createPlanet(context);
        }
    }

    public void createPlanet(SystemCreationContext context) {
        PlanetSource planetSource = PlanetSource.createRandom(context.random);
        PlanetData planetData = PlanetData.fromPlanetSource(planetSource, context.nextPlanetName());

        Holder<DimensionType> dimensionTypeHolder = createAndRegisterDimensionType(context, planetData);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = createAndRegisterBiomes(context, planetData);

        NoiseGeneratorSettings noiseSettings = TerrainGenCreator.createFromPlanetData(planetData, context);

        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeList));
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(biomeSource, Holder.direct(noiseSettings));
        LevelStem stem = new LevelStem(dimensionTypeHolder, noiseBasedChunkGenerator);

        RegistryUtil.registerLevelStem(context.server, ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name()), stem);
    }

    private Holder<DimensionType> createAndRegisterDimensionType(SystemCreationContext context, PlanetData planetData) {
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

    private List<Pair<Climate.ParameterPoint, Holder<Biome>>> createAndRegisterBiomes(SystemCreationContext context, PlanetData planetData) {
        int biomeCount = 3 + context.random.nextInt(4);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new ArrayList<>();

        for (int i = 0; i < biomeCount; i++) {
            double variationFactor = (double) i / biomeCount;

            Biome biome = BiomeCreator.createBiome(context.random, planetData, variationFactor);

            ResourceLocation biomeLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name() + "_biome_" + i);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeLocation);
            RegistryUtil.registerBiome(context.server, biomeLocation, biome);

            // Create climate parameters for this biome based on variation
            Climate.ParameterPoint climateParams = createClimateParameters(planetData, variationFactor);

            // Create holder for the biome
            Holder<Biome> biomeHolder = context.server.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(biomeKey);

            biomeList.add(Pair.of(climateParams, biomeHolder));
        }

        return biomeList;
    }

    /**
     * Create climate parameters for a biome based on planet data and variation factor
     */
    private Climate.ParameterPoint createClimateParameters(PlanetData planetData, double variationFactor) {
        // Base values from planet data, varied by the biome's variation factor
        float temperature = (float) (planetData.temperature() + (variationFactor - 0.5) * 0.4);
        float humidity = (float) ((planetData.atmosphericDensity() + planetData.seaLevel()) / 2.0);
        float continentalness = (float) (planetData.size() - 1.0); // Size affects landmass
        float erosion = (float) (1.0 - planetData.terrainRoughness()); // Rough terrain = less erosion
        float depth = 0.0f; // Depth parameter
        float weirdness = (float) (planetData.weirdness() - 1.0);

        // Create climate parameter ranges (using single points for simplicity)
        return Climate.parameters(
                Climate.Parameter.point(temperature),
                Climate.Parameter.point(humidity),
                Climate.Parameter.point(continentalness),
                Climate.Parameter.point(erosion),
                Climate.Parameter.point(depth),
                Climate.Parameter.point(weirdness),
                0L // offset - could be based on biome index
        );
    }


    public static class SystemCreationContext {
        public final MinecraftServer server;
        public final RandomSource random = RandomSource.create();
        public final String systemName;
        public int currentPlanetIndex = 0;

        public SystemCreationContext(MinecraftServer server, boolean scientificNameStyle) {
            this.server = server;
            this.systemName = SystemNameGenerator.get(scientificNameStyle).generate(random);
        }

        public String nextPlanetName() {
            return systemName + SystemNameGenerator.ALL_LETTERS.charAt(currentPlanetIndex++);
        }
    }
}
