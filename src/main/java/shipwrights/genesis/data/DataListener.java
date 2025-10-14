package shipwrights.genesis.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import g_mungus.vlib.data.DimensionSettings;
import g_mungus.vlib.dimension.DimensionSettingsManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            ModList modList = ModList.get();

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {

                    SystemConfigModel config = GSON.fromJson(reader, SystemConfigModel.class);

                    if (config != null && config.planets() != null) {
                        GenesisMod.LOGGER.info("Loading {} planets from {}", config.planets().size(), location);

                        for (SystemConfigModel.PlanetJsonModel planet : config.planets()) {
                            String mod = planet.getDimensionID().getNamespace();
                            if (!modList.isLoaded(mod) && COMPAT_MODS.contains(mod)) {
                                continue;
                            }

                            GenesisMod.registerPlanet(
                                planet.getDimensionID(),
                                    planet.size(),
                                    planet.orbitRadius(),
                                    planet.yearLength(),
                                    planet.r(),
                                    planet.g(),
                                    planet.b()
                            );

                            DimensionSettingsManager.INSTANCE.addSettings(planet.getDimensionID(), new DimensionSettings(1.0, planet.gravity(), true));
                            GenesisMod.LOGGER.info("Registered planet from data: {}", planet.dimensionID());
                        }

                        for (SystemConfigModel.MoonJsonModel moon : config.moons()) {
                            String mod = moon.getDimensionID().getNamespace();
                            if (!modList.isLoaded(mod) && COMPAT_MODS.contains(mod)) {
                                continue;
                            }

                            GenesisMod.registerMoon(
                                    moon.getDimensionID(),
                                    moon.getParentDimensionID(),
                                    moon.size(),
                                    moon.orbitRadius(),
                                    moon.yearLength(),
                                    moon.r(),
                                    moon.g(),
                                    moon.b()
                            );

                            DimensionSettingsManager.INSTANCE.addSettings(moon.getDimensionID(), new DimensionSettings(1.0, moon.gravity(), true));
                            GenesisMod.LOGGER.info("Registered moon from data: {}", moon.dimensionID());
                        }
                    }
                } catch (Exception e) {
                    GenesisMod.LOGGER.error("Failed to load system_config data from: {}", location, e);
                }
            }

            GenesisMod.finalizeMoons();
        }
    }

    private static final Set<String> COMPAT_MODS = Set.of("ad_astra");
}

