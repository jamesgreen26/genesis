package shipwrights.genesis.space;

import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

public sealed interface Orbitable {

    double BASE_SIZE = 96;
    double BASE_ORBIT_DISTANCE = 15_000;
    double BASE_ORBIT_TIME = 4_608_000;

    ResourceLocation getID();

    default boolean exists() {
        return GenesisMod.SPACE_REGISTRY.isRegistered(this);
    }

    Vector3dc getCurrentPos(long ticks, float subticks);
    default Vector3dc getCurrentPos(long ticks) {
        return getCurrentPos(ticks, 0f);
    }


    abstract sealed class Celestial implements Orbitable permits OrbitingBody, Star {

        public abstract double size();

        public double getActualSize() {
            return this.size() * BASE_SIZE;
        }

        public abstract Quaterniondc getRotation(long ticks, float subticks);
        public Quaterniondc getRotation(long ticks) { return getRotation(ticks, 0f); }

        public static class WithDistanceSq<T extends Celestial> {
            public final T celestial;
            public final double distanceSquared;

            public WithDistanceSq(T celestial, double distanceSquared) {
                this.celestial = celestial;
                this.distanceSquared = distanceSquared;
            }

            public double getDistanceSquared() {
                return distanceSquared;
            }

            public T getCelestial() {
                return celestial;
            }
        }
    }
}
