package shipwrights.dataplanets.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.genesis.data.SystemConfigModel;
import shipwrights.genesis.planets.PlanetData;
import shipwrights.dataplanets.util.Color;
import shipwrights.genesis.GenesisMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenesisCompat implements Compat{
    @Override
    public void addPlanetsToSpace(MinecraftServer server, List<shipwrights.dataplanets.systemCreation.PlanetData> planets) {
        List<PlanetData> genesisPlanets = new ArrayList<>();

        for (var planetData : planets) {
            Color color = planetData.getPrimaryColour();

            Optional<shipwrights.genesis.planets.PlanetData> genesisPlanetData = GenesisMod.registerPlanet(
                    ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, planetData.name()),
                    planetData.size(),
                    planetData.gravity(),
                    planetData.distanceFromStar(),
                    planetData.orbitalPeriod(),
                    color.red(),
                    color.blue(),
                    color.green()
                );

            genesisPlanetData.ifPresent(genesisPlanets::add);
        }

        try {
            writeToDataPack(genesisPlanets, server);
        } catch (IOException e) {
            DataplanetsMod.LOGGER.error("Failed to write Genesis planets to datapack", e);
        }
    }

    private void writeToDataPack(List<PlanetData> planets, MinecraftServer server) throws IOException {
        Path basePath = server.storageSource.getLevelPath(LevelResource.DATAPACK_DIR);
        Path planetsPath = basePath.resolve("genesis/system_config");
        String fileName = "dataplanets.json";

        SystemConfigModel systemConfig = new SystemConfigModel(planets.stream().map(PlanetData::toJsonModel).toList(), List.of());

        Files.createDirectories(planetsPath);

        JsonElement json = SystemConfigModel.CODEC.encodeStart(JsonOps.INSTANCE, systemConfig)
                .resultOrPartial(error -> DataplanetsMod.LOGGER.error("Failed to encode system config: {}", error))
                .orElseThrow(() -> new IOException("Failed to encode system config"));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(json);

        Path filePath = planetsPath.resolve(fileName);
        Files.writeString(filePath, jsonString);

        DataplanetsMod.LOGGER.info("Wrote {} planets to {}", planets.size(), filePath);
    }


    @Override
    public ResourceLocation getSpaceDimensionEffects() {
        return ResourceLocation.parse("genesis:great_unknown");
    }
}
