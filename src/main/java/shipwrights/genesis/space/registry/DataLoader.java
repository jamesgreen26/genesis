package shipwrights.genesis.space.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataLoader {
    private static final List<SystemConfigModel> loadedConfigs = new CopyOnWriteArrayList<>();

    static {
        GenesisMod.onRegisterCelestialsEvent(event -> {
            for (SystemConfigModel config : loadedConfigs) {
                config.celestials().forEach(event::accept);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) resourceManager -> {
            loadedConfigs.clear();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("system_config", location -> true);

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {

                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    SystemConfigModel config = SystemConfigModel.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                            .getOrThrow(false, SpaceRegistry.LOGGER::error);

                    loadedConfigs.add(config);

                } catch (Exception e) {
                    SpaceRegistry.LOGGER.error("Failed to load system_config data from: {}", entry.getKey(), e);
                }
            }

            GenesisMod.SPACE_REGISTRY.bake();
        });
    }
}
