package shipwrights.genesis.space;

import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.Optional;

/// Represents where the observer is in space, for purposes such as rendering celestials.
public sealed interface VantagePoint {

    static VantagePoint get(Level level, Time time, Vector3dc pos) {
        if (GenesisMod.isSpaceDimension(level)) {
            return InSpace.INSTANCE;
        } else {
            Optional<Celestial> celestial = GenesisMod.celestial(level);
            if (celestial.isEmpty()) {
                return VantagePoint.SomewhereElse.INSTANCE;
            } else {
                Registry<Celestial> celestials = level.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
                return new OnCelestial(celestials, celestial.get(), time, 0, 0); // TODO: planet projections
            }
        }
    }

    boolean isReal();
    Vector3dc getPosition();
    Quaterniondc getRotation();

    record InSpace () implements VantagePoint {
        static final InSpace INSTANCE = new InSpace();

        @Override
        public boolean isReal() { return true; }

        @Override
        public Vector3dc getPosition() {
            return new Vector3d();
        }

        @Override
        public Quaterniondc getRotation() {
            return new Quaterniond();
        }
    }

    /// Standing on the surface of a cube-shaped celestial.
    ///
    /// Latitude and longitude (radians) pick a direction from the celestial's centre, in its local space:
    /// latitude {@code π/2} points along +Y, and latitude 0, longitude 0 along -Z. Longitude increases eastward,
    /// turning right-handed about +Y (-Z towards -X). The observer stands where that direction meets the cube's
    /// surface, with "up" being the normal of the face they're standing on, and north facing the +Y pole.
    ///
    /// The observer's local frame follows Minecraft's conventions: +X east, +Y up, +Z south.
    record OnCelestial (
            Registry<Celestial> celestials,
            Celestial celestial,
            Time time,
            double latitude,
            double longitude
    ) implements VantagePoint {

        @Override
        public boolean isReal() {
            return true;
        }

        @Override
        public Vector3dc getPosition() {
            Vector3d direction = direction();
            double halfSize = celestial.size() / 2.0;
            Vector3d absolute = direction.absolute(new Vector3d());
            Vector3d surface = direction.mul(halfSize / absolute.get(absolute.maxComponent()));

            return celestial.transformProvider().getRotation(celestials, time)
                    .transform(surface)
                    .add(celestial.transformProvider().getPosition(celestials, time));
        }

        @Override
        public Quaterniondc getRotation() {
            return new Quaterniond(celestial.transformProvider().getRotation(celestials, time))
                    .mul(localRotation());
        }

        /// Rotation from the observer's frame into the celestial's local space.
        private Quaterniond localRotation() {
            Vector3d direction = direction();
            int axis = direction.absolute(new Vector3d()).maxComponent();
            Vector3d up = new Vector3d().setComponent(axis, Math.signum(direction.get(axis)));

            // Direction of increasing longitude, flattened onto the face. Never parallel to the face normal,
            // since that would require the normal's component of the direction to be zero.
            Vector3d east = new Vector3d(-Math.cos(longitude), 0, Math.sin(longitude));
            east.fma(-east.dot(up), up).normalize();
            Vector3d south = east.cross(up, new Vector3d());

            return new Quaterniond().setFromNormalized(new Matrix3d(east, up, south));
        }

        private Vector3d direction() {
            double cosLatitude = Math.cos(latitude);
            return new Vector3d(
                    -cosLatitude * Math.sin(longitude),
                    Math.sin(latitude),
                    -cosLatitude * Math.cos(longitude)
            );
        }
    }

    final class SomewhereElse implements VantagePoint {
        static final SomewhereElse INSTANCE = new SomewhereElse();

        @Override
        public boolean isReal() {
            return false;
        }

        @Override
        public Vector3dc getPosition() {
            return new Vector3d();
        }

        @Override
        public Quaterniondc getRotation() {
            return new Quaterniond();
        }
    }
}
