package g_mungus.genesis;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;

import java.util.HashMap;
import java.util.Map;

public class PlanetRegistry {
    private static final Map<ResourceLocation, PlanetData> planets = new HashMap();

    public static void registerPlanet(ResourceLocation dimensionID, int size, Vector3ic location) {
        planets.put(dimensionID, new PlanetData(size, location));
    }

    @Nullable
    public static PlanetData getPlanetData(ResourceLocation dimensionID) {
        return planets.getOrDefault(dimensionID, null);
    }

    public record PlanetData(int size, Vector3ic location) {}
}
