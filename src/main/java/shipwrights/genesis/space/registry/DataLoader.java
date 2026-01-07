package shipwrights.genesis.space.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shipwrights.genesis.GenesisMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DataLoader {
    @SubscribeEvent
    public static void onRegisterReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) resourceManager -> {
            GenesisMod.SPACE_REGISTRY.reset();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("system_config", location -> true);

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {

                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    SystemConfigModel config = SystemConfigModel.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                            .getOrThrow(false, SpaceRegistry.LOGGER::error);

                    GenesisMod.onRegisterCelestialsEvent(registerCelestialsEvent -> {
                        config.stars().forEach(it -> registerCelestialsEvent.accept(it.getID(), it));
                        config.bodies().forEach(it -> registerCelestialsEvent.accept(it.getID(), it));
                    });

                } catch (Exception e) {
                    SpaceRegistry.LOGGER.error("Failed to load system_config data from: {}", entry.getKey(), e);
                }
            }
            GenesisMod.SPACE_REGISTRY.bake();
        });
    }
}
