package shipwrights.dataplanets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.PlanetSource;
import shipwrights.dataplanets.util.DataPackUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class PlanetLookup {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, PlanetData> planets = new ConcurrentHashMap<>();

    public static void store(MinecraftServer server, PlanetSource planet) {
        load(planet);
        DataPackUtil.write(server, "dataplanets/planet_source", planet.name() + ".json", planet, PlanetSource.CODEC);
    }

    private static void load(PlanetSource planet) {
        planets.put(id(planet.name()), PlanetData.fromPlanetSource(planet));
    }

    public static PlanetData get(ResourceLocation dimension) {
        return planets.getOrDefault(dimension, getDefault(dimension.getPath()));
    }

    private static ResourceLocation id(String dimensionName) {
        return ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, dimensionName);
    }

    private static PlanetData getDefault(String name) {
        return new PlanetData(name, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, ResourceLocation.withDefaultNamespace("stone"), ResourceLocation.withDefaultNamespace("water"), 1.0);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(GSON, "dataplanets/planet_source") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                    try {
                        PlanetSource.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                                .resultOrPartial(error -> DataplanetsMod.LOGGER.error("Failed to parse planet source {}: {}", entry.getKey(), error)).ifPresent(PlanetLookup::load);

                    } catch (Exception e) {
                        DataplanetsMod.LOGGER.error("Error loading planet source {}", entry.getKey(), e);
                    }
                }
            }
        });
    }
}
