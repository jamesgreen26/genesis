package shipwrights.genesis.space.star_properties;

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
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataLoader {

    @SubscribeEvent
    public static void onRegisterReloadListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) resourceManager -> {
            StarProperties.reset();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    "system_config/star_properties", location -> location.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {

                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    StarPropertiesModel model = StarPropertiesModel.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                            .getOrThrow(false, GenesisMod.LOGGER::error);

                    for (StarProperties props : model.stars()) {
                        StarProperties.register(props.id(), props);
                    }

                } catch (Exception e) {
                    GenesisMod.LOGGER.error("Failed to load star_properties from: {}", entry.getKey(), e);
                }
            }

            StarPropertiesSyncPacket.sendToAllClients();
        });
    }
}
