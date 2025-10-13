package shipwrights.genesis.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataListener {

    @SubscribeEvent
    public static void onRegisterReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    public static class Listener implements ResourceManagerReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("system_config", location -> true);

            if (resources.isEmpty()) {
                GenesisMod.LOGGER.warn("Could not find any system_config.json files. Skipping planet data loading.");
                return;
            }

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {

                    SystemConfigModel config = GSON.fromJson(reader, SystemConfigModel.class);

                    if (config != null && config.planets() != null) {
                        GenesisMod.LOGGER.info("Loading {} planets from {}", config.planets().size(), location);

                        for (SystemConfigModel.PlanetJsonModel planet : config.planets()) {
                            GenesisMod.registerPlanet(
                                planet.getDimensionID(),
                                    planet.size(),
                                    planet.sunDist(),
                                    planet.yearLength(),
                                    planet.r(),
                                    planet.g(),
                                    planet.b()
                            );
                            GenesisMod.LOGGER.info("Registered planet from data: {}", planet.dimensionID());
                        }
                    }
                } catch (Exception e) {
                    GenesisMod.LOGGER.error("Failed to load system_config data from: {}", location, e);
                }
            }
        }
    }
}

