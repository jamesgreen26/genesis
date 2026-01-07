package shipwrights.dataplanets.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.util.RegistryUtil;
import shipwrights.dataplanets.util.Color;
import shipwrights.genesis.GenesisMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenesisCompat implements Compat{
    @Override
    public void addPlanetsToSpace(MinecraftServer server, List<shipwrights.dataplanets.systemCreation.PlanetData> planets) {
//        List<PlanetData> genesisPlanets = new ArrayList<>();
//
//        for (var planetData : planets) {
//            Color color = planetData.getPrimaryColour();
//
//            Optional<shipwrights.genesis.planets.PlanetData> genesisPlanetData = GenesisMod.registerPlanet(
//                    ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, planetData.name()),
//                    planetData.size(),
//                    planetData.gravity(),
//                    planetData.distanceFromStar(),
//                    planetData.orbitalPeriod(),
//                    color.red() / 255.0f,
//                    color.green() / 255.0f,
//                    color.blue() / 255.0f
//                );
//
//            genesisPlanetData.ifPresent(genesisPlanets::add);
//        }
//
//        GalaxySerializable systemConfig = new GalaxySerializable(
//                genesisPlanets.stream().map(PlanetData::toJsonModel).toList(),
//                List.of()
//        );
//
//        RegistryUtil.writeToDatapack(server, ResourceLocation.parse("genesis:dataplanets"), "system_config", GalaxySerializable.CODEC, systemConfig);
    }

    @Override
    public ResourceLocation getSpaceDimensionEffects() {
        return ResourceLocation.parse("genesis:great_unknown");
    }
}
