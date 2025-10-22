package shipwrights.dataplanets.planetCreation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.dimension.DimensionType;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.naming.SystemNameGenerator;
import shipwrights.dataplanets.util.RegistryUtil;

public class SystemCreator {

    public void createSystem(MinecraftServer server, boolean scientificNamingStyle) {
        SystemCreationContext context = new SystemCreationContext(server, scientificNamingStyle);
    }

    public void createPlanet(SystemCreationContext context) {
        PlanetSource planetSource = PlanetSource.createRandom(context.random);
        PlanetData planetData = PlanetData.fromPlanetSource(planetSource, context.nextPlanetName());

        DimensionType dimensionType = createAndRegisterDimensionType(context, planetData);
    }

    private DimensionType createAndRegisterDimensionType(SystemCreationContext context, PlanetData planetData) {
        DimensionType dimensionType = BuiltinDimensionTypes.createFromPlanetData(planetData);
        RegistryUtil.registerDimensionType(
                context.server,
                ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, planetData.name() + "_dimension_type"),
                dimensionType
        );
        return dimensionType;
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
