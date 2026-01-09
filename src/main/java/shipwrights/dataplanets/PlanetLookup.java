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
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.PlanetSource;
import shipwrights.dataplanets.util.Color;
import shipwrights.dataplanets.runtimeRegistration.RegistryUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class PlanetLookup {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, PlanetData> planets = new ConcurrentHashMap<>();
    private static final String PATH = "planet_source";

    public static void store(MinecraftServer server, PlanetSource planet) {
        load(planet);
        RegistryUtil.writeToDatapack(server, id(planet.name()), PATH, PlanetSource.CODEC, planet);
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
        return new PlanetData(name, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, ResourceLocation.withDefaultNamespace("stone"), ResourceLocation.withDefaultNamespace("water"), 1.0, new Color(128, 128, 128, 255));
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(GSON, PATH) {
            @Override
            protected void apply(@NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
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

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        planets.clear();
    }
}
