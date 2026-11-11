package shipwrights.genesis.space.travel;

import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/// Lays a cube-shaped celestial's surface out flat, as the map its dimension's X/Z plane is: the north (-Z) face is a
/// square in the middle, the up, down, east and west faces are trapezoids around it, and the south face is a square
/// ring around those, its centre stretched over the map's outer edge. Across the middle, the map is half the south
/// face, a trapezoid, the north square, a trapezoid and the other half of the south face, the halves of the south face
/// each half as wide as the rest, so every face is as wide across as every other.
///
/// The map is in units of a quarter of the north square's width: it runs from -4 to 4 on each axis, the north square
/// from -1 to 1. The cube is the one of half size 1 in the celestial's own space. As in
/// {@link shipwrights.genesis.space.VantagePoint}, the map's origin is the centre of the north face, the map's east (+X)
/// is the celestial's -X there, and the map's north (-Z) is towards its +Y pole, so the up face lies to the north.
///
/// Measured from the map's origin by the larger of its two coordinates, `m`, a point is `m` across the north face
/// while `m` is up to 1, `m - 1` across the face beyond while up to 3, and the rest of the way to the south face's
/// centre after.
public final class CelestialProjection {

    /// How far the map runs from its origin on each axis
    public static final double HALF_WIDTH = 4;

    private static final double NORTH_EDGE = 1;
    private static final double SOUTH_EDGE = 3;

    private CelestialProjection() {
    }

    /// The point on the cube's surface where {@code direction}, from the celestial's centre, meets it, on the map
    ///
    /// @param direction in the celestial's own space; the zero vector is taken to be towards the north face
    public static Vector2d toMap(Vector3dc direction) {
        double max = Math.max(Math.abs(direction.x()), Math.max(Math.abs(direction.y()), Math.abs(direction.z())));
        if (max == 0) return new Vector2d();
        double x = direction.x() / max;
        double y = direction.y() / max;
        double z = direction.z() / max;

        boolean onZFace = Math.abs(z) >= Math.abs(x) && Math.abs(z) >= Math.abs(y);
        if (onZFace && z < 0) {
            return new Vector2d(-x, -y);
        }

        // On the side faces and the south face, (x, y) / max(|x|, |y|) is the direction out from the map's origin,
        // mirrored: on a side face it's (x, y) itself, one of them being ±1
        double m;
        double across = Math.max(Math.abs(x), Math.abs(y));
        if (onZFace) {
            m = HALF_WIDTH - across;
            // The south face's centre is the whole of the map's edge; take the middle of its north side
            if (across == 0) return new Vector2d(0, -HALF_WIDTH);
        } else {
            m = z + 2;
        }
        return new Vector2d(-m * x / across, -m * y / across);
    }

    /// The point on the cube's surface at {@code (x, z)} on the map, clamped to the map, in the celestial's own space
    public static Vector3d toCube(double mapX, double mapZ) {
        double x = clamp(mapX);
        double z = clamp(mapZ);
        double m = Math.max(Math.abs(x), Math.abs(z));
        if (m == 0) return new Vector3d(0, 0, -1);

        // How far across the cube's face from its centre axis, and along that axis
        double across = Math.min(m, Math.min(NORTH_EDGE, HALF_WIDTH - m));
        double along = Math.max(-1, Math.min(1, m - 2));
        return new Vector3d(-x / m * across, -z / m * across, along);
    }

    /// The rotation from a dimension's own frame (+X east, +Y up, +Z south) at {@code (x, z)} on its map to the
    /// celestial's own space. Up is the outward normal of the face that point is on.
    ///
    /// It's the same over each of nine regions of the map: the north square; each trapezoid; and each quarter of the
    /// south ring, between the diagonals of the south face. The map's axes keep their directions on the cube across the
    /// edge from the north square to each trapezoid, and from each trapezoid to the south ring beyond it.
    public static Quaterniond frame(double mapX, double mapZ) {
        double x = clamp(mapX);
        double z = clamp(mapZ);
        double m = Math.max(Math.abs(x), Math.abs(z));
        boolean xOutward = Math.abs(x) >= Math.abs(z);

        // Where the map's +X and +Z point on the cube
        Vector3d east;
        Vector3d south;
        if (m <= NORTH_EDGE) {
            east = new Vector3d(-1, 0, 0);
            south = new Vector3d(0, -1, 0);
        } else if (m < SOUTH_EDGE) {
            // Outward on the map is away from the north face, towards the south face
            if (xOutward) {
                east = new Vector3d(0, 0, Math.signum(x));
                south = new Vector3d(0, -1, 0);
            } else {
                east = new Vector3d(-1, 0, 0);
                south = new Vector3d(0, 0, Math.signum(z));
            }
        } else {
            // Outward on the map is on towards the south face's centre, the opposite way to on the north face
            if (xOutward) {
                east = new Vector3d(1, 0, 0);
                south = new Vector3d(0, -1, 0);
            } else {
                east = new Vector3d(-1, 0, 0);
                south = new Vector3d(0, 1, 0);
            }
        }
        Vector3d up = south.cross(east, new Vector3d());
        return new Quaterniond().setFromNormalized(new Matrix3d(east, up, south));
    }

    private static double clamp(double coordinate) {
        return Math.max(-HALF_WIDTH, Math.min(HALF_WIDTH, coordinate));
    }
}
