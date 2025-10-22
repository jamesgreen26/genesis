package shipwrights.dataplanets.systemCreation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.naming.SystemNameGenerator;
import shipwrights.dataplanets.systemCreation.dimension.BiomeCreator;
import shipwrights.dataplanets.systemCreation.dimension.DimensionTypeCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.ArrayList;
import java.util.List;

public class SystemCreator {

    public void createSystem(MinecraftServer server, boolean scientificNamingStyle) {
        SystemCreationContext context = new SystemCreationContext(server, scientificNamingStyle);
    }

    public void createPlanet(SystemCreationContext context) {
        PlanetSource planetSource = PlanetSource.createRandom(context.random);
        PlanetData planetData = PlanetData.fromPlanetSource(planetSource, context.nextPlanetName());

        DimensionType dimensionType = createAndRegisterDimensionType(context, planetData);

        List<Biome> biomes = createAndRegisterBiomes(context, planetData);
    }

    private DimensionType createAndRegisterDimensionType(SystemCreationContext context, PlanetData planetData) {
        DimensionType dimensionType = DimensionTypeCreator.createFromPlanetData(planetData);
        RegistryUtil.registerDimensionType(
                context.server,
                ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, planetData.name() + "_dimension_type"),
                dimensionType
        );
        return dimensionType;
    }

    private List<Biome> createAndRegisterBiomes(SystemCreationContext context, PlanetData planetData) {
        int biomeCount = 3 + context.random.nextInt(4);
        List<Biome> biomes = new ArrayList<>();

        for (int i = 0; i < biomeCount; i++) {
            double variationFactor = (double) i / biomeCount;

            Biome biome = BiomeCreator.createBiome(context.random, planetData, variationFactor);

            ResourceLocation biomeLocation = ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, planetData.name() + "_biome_" + i);
            RegistryUtil.registerBiome(context.server, biomeLocation, biome);

            biomes.add(biome);
        }

        return biomes;
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
