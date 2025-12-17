package shipwrights.genesis.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;

/**
 * Utility class for planet-related operations in Genesis
 */
public class PlanetUtil {

    /**
     * Get the space dimension (Great Unknown) where planets orbit
     */
    public static ResourceKey<Level> getSpaceDimension() {
        return ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            GenesisMod.SPACE_DIM
        );
    }

    /**
     * Check if a dimension is the space dimension
     */
    public static boolean isSpaceDimension(ResourceKey<Level> dimension) {
        return dimension.location().equals(GenesisMod.SPACE_DIM);
    }

    /**
     * Get planet data for a specific dimension
     */
    @Nullable
    public static PlanetData getPlanetByDimension(ResourceKey<Level> dimension) {
        return GenesisMod.planets.get(dimension.location());
    }

    /**
     * Get the nearest planet to a position in space
     */
    public static Optional<PlanetWithDistance> getNearestPlanet(Vec3 position, long ticks) {
        return GenesisMod.planets.values().stream()
            .map(planet -> {
                Vector3d planetPos = planet.getCurrentPos(ticks);
                double distance = Math.sqrt(
                    Math.pow(position.x - planetPos.x, 2) +
                    Math.pow(position.y - planetPos.y, 2) +
                    Math.pow(position.z - planetPos.z, 2)
                );
                return new PlanetWithDistance(planet, distance);
            })
            .min(Comparator.comparingDouble(pwd -> pwd.distance))
            .filter(pwd -> pwd.distance < Double.MAX_VALUE);
    }


    /**
     * Get the rotation quaternion for a planet
     */
    public static Quaterniond getPlanetRotation(PlanetData planet) {
        // Convert the planet's rotation vector to a quaternion
        // For now, using identity - you may want to implement proper rotation from the Vector3d
        return new Quaterniond(); // TODO: Implement proper rotation conversion from planet.rot
    }

    /**
     * Helper record to store planet with calculated distance
     */
    public record PlanetWithDistance(PlanetData planet, double distance) {}
}
