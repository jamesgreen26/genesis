package shipwrights.dataplanets.systemCreation;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
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
import shipwrights.dataplanets.systemCreation.naming.SystemNameGenerator;
import shipwrights.dataplanets.systemCreation.dimension.biome.BiomeCreator;
import shipwrights.dataplanets.systemCreation.dimension.DimensionTypeCreator;
import shipwrights.dataplanets.systemCreation.dimension.noise.TerrainGenCreator;
import shipwrights.dataplanets.util.RegistryUtil;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

public class SystemCreator {

    public void createSystem(MinecraftServer server, boolean scientificNamingStyle) {
        SystemCreationContext context = new SystemCreationContext(server, scientificNamingStyle);

        List<PlanetSource> sources = createPlanetSources(context);

        for (var source : sources) {
            createPlanet(source, context);
        }
    }

    private static List<PlanetSource> createPlanetSources(SystemCreator.SystemCreationContext context) {
        int planetCount = context.random.nextInt(4, 9);
        List<PlanetSource> output = new ArrayList<>();
        for (int i = 0; i < planetCount; i++) {
            output.add(PlanetSource.createRandom(context.nextPlanetName(), context.random));
        }

        return output;
    }

    public void createPlanet(PlanetSource source, SystemCreationContext context) {
        PlanetData planetData = PlanetData.fromPlanetSource(source);

        Holder<DimensionType> dimensionTypeHolder = DimensionTypeCreator.createAndRegisterDimensionType(context, planetData);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new BiomeCreator().createAndRegisterBiomes(context, planetData);

        Holder<NoiseGeneratorSettings> noiseSettings = TerrainGenCreator.createFromPlanetData(planetData, context);

        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeList));
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

        LevelStem stem = new LevelStem(dimensionTypeHolder, noiseBasedChunkGenerator);

        RegistryUtil.registerLevelStem(context.server, ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name()), stem);
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
