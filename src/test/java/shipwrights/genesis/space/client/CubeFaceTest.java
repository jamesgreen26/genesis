package shipwrights.genesis.space.client;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CubeFaceTest {
    /// Each face's corners, bottom left, bottom right, top right and top left, then its UVs, as the cube was drawn
    /// before {@link CubeFace} was taken out of it
    private static final Map<CubeFace, float[]> AS_DRAWN = Map.of(
            CubeFace.SOUTH, new float[]{-1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, 2 / 3f, 0, 1, 0.5f},
            CubeFace.NORTH, new float[]{1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 0, 0, 1 / 3f, 0.5f},
            CubeFace.WEST, new float[]{-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, 1 / 3f, 0, 2 / 3f, 0.5f},
            CubeFace.EAST, new float[]{1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1, 0, 0.5f, 1 / 3f, 1},
            CubeFace.DOWN, new float[]{-1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1, 1 / 3f, 0.5f, 2 / 3f, 1},
            CubeFace.UP, new float[]{-1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1, 2 / 3f, 0.5f, 1, 1});

    @Test
    void facesAreWhereTheCubeHasAlwaysBeenDrawn() {
        for (CubeFace face : CubeFace.values()) {
            float[] expected = AS_DRAWN.get(face);
            Vector3f[] corners = {face.point(0, 1), face.point(1, 1), face.point(1, 0), face.point(0, 0)};
            for (int i = 0; i < 4; i++) {
                assertEquals(new Vector3f(expected[i * 3], expected[i * 3 + 1], expected[i * 3 + 2]), corners[i],
                        face + " corner " + i);
            }
            assertEquals(expected[12], face.u0(), face + " u0");
            assertEquals(expected[13], face.v0(), face + " v0");
            assertEquals(expected[14], face.u1(), face + " u1");
            assertEquals(expected[15], face.v1(), face + " v1");
        }
    }

    @Test
    void stepsOffEveryEdgeLandOnAPixelOfTheNextFace() {
        int size = 16;
        for (CubeFace face : CubeFace.values()) {
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    for (int[] step : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                        int c = column + step[0], r = row + step[1];
                        if (c >= 0 && c < size && r >= 0 && r < size) continue;
                        Neighbour neighbour = acrossEdge(face, size, c, r);
                        assertTrue(neighbour.face() != face, () -> face + " stays on itself");
                        // On a pixel's middle, so the faces' pixels line up along the edge
                        assertEquals(Math.floor(neighbour.s() * size) + 0.5, neighbour.s() * size, 1e-9);
                        assertEquals(Math.floor(neighbour.t() * size) + 0.5, neighbour.t() * size, 1e-9);
                    }
                }
            }
        }
    }

    /// Where on the cube the pixel at {@code column}, {@code row} would be, just off {@code face}, folded over the edge
    /// onto the face it's on
    public record Neighbour(CubeFace face, double s, double t) {
        public int column(int size) {
            return (int) Math.floor(s * size);
        }

        public int row(int size) {
            return (int) Math.floor(t * size);
        }
    }

    public static Neighbour acrossEdge(CubeFace face, int size, int column, int row) {
        double s = (column + 0.5) / size, t = (row + 0.5) / size;
        double[] point = {face.x(s, t), face.y(s, t), face.z(s, t)};
        double[] centre = {face.x(0.5, 0.5), face.y(0.5, 0.5), face.z(0.5, 0.5)};
        int normal = axisOf(centre);
        for (int axis = 0; axis < 3; axis++) {
            double excess = Math.abs(point[axis]) - 1;
            if (axis == normal || excess <= 0) continue;
            point[axis] = Math.signum(point[axis]);
            point[normal] -= Math.signum(point[normal]) * excess;
            for (CubeFace other : CubeFace.values()) {
                double[] otherCentre = {other.x(0.5, 0.5), other.y(0.5, 0.5), other.z(0.5, 0.5)};
                if (axisOf(otherCentre) != axis || otherCentre[axis] != point[axis]) continue;
                double[] origin = {other.x(0, 0), other.y(0, 0), other.z(0, 0)};
                double[] across = {other.x(1, 0) - origin[0], other.y(1, 0) - origin[1], other.z(1, 0) - origin[2]};
                double[] down = {other.x(0, 1) - origin[0], other.y(0, 1) - origin[1], other.z(0, 1) - origin[2]};
                double os = 0, ot = 0;
                for (int i = 0; i < 3; i++) {
                    os += (point[i] - origin[i]) * across[i] / 4;
                    ot += (point[i] - origin[i]) * down[i] / 4;
                }
                return new Neighbour(other, os, ot);
            }
        }
        throw new AssertionError("Nowhere to go from " + face + " " + column + ", " + row);
    }

    private static int axisOf(double[] point) {
        for (int axis = 0; axis < 3; axis++) if (Math.abs(point[axis]) == 1) return axis;
        throw new AssertionError();
    }
}
