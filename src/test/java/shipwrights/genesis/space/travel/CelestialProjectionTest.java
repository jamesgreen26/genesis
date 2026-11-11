package shipwrights.genesis.space.travel;

import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialProjectionTest {

    private static final double EPSILON = 1e-9;

    @Test
    void originIsNorthFaceCentre() {
        assertVector(new Vector3d(0, 0, -1), CelestialProjection.toCube(0, 0));
    }

    @Test
    void mapAxesMatchVantagePointOnNorthFace() {
        Quaterniond frame = CelestialProjection.frame(0, 0);
        assertVector(new Vector3d(-1, 0, 0), frame.transform(new Vector3d(1, 0, 0)));
        assertVector(new Vector3d(0, 0, -1), frame.transform(new Vector3d(0, 1, 0)));
        // North (-Z on the map) is towards the +Y pole
        assertVector(new Vector3d(0, 1, 0), frame.transform(new Vector3d(0, 0, -1)));
    }

    @Test
    void everyFaceIsAsWideAcrossTheMiddle() {
        // North square from -1 to 1, east trapezoid 1 to 3, half the south face 3 to 4
        assertVector(new Vector3d(-1, 0, -1), CelestialProjection.toCube(1, 0));
        assertVector(new Vector3d(-1, 0, 1), CelestialProjection.toCube(3, 0));
        assertVector(new Vector3d(0, 0, 1), CelestialProjection.toCube(4, 0));
        // Halfway across the east trapezoid is halfway along the east face
        assertVector(new Vector3d(-1, 0, 0), CelestialProjection.toCube(2, 0));
        // The up face lies to the north
        assertVector(new Vector3d(0, 1, 0), CelestialProjection.toCube(0, -2));
    }

    @Test
    void sideFacesAreTrapezoids() {
        // The up face's north edge is 2 wide on the map, its south edge 6
        assertVector(new Vector3d(-1, 1, -1), CelestialProjection.toCube(1, -1));
        assertVector(new Vector3d(-1, 1, 1), CelestialProjection.toCube(3, -3));
    }

    @Test
    void southFaceCentreIsTheWholeMapEdge() {
        for (double t = -4; t <= 4; t += 0.5) {
            assertVector(new Vector3d(0, 0, 1), CelestialProjection.toCube(t, 4));
            assertVector(new Vector3d(0, 0, 1), CelestialProjection.toCube(4, t));
        }
    }

    @Test
    void clampsToTheMap() {
        assertVector(CelestialProjection.toCube(4, 2), CelestialProjection.toCube(100, 2));
        assertVector(CelestialProjection.toCube(-4, -0.5), CelestialProjection.toCube(-100, -0.5));
        Quaterniond clamped = CelestialProjection.frame(-100, -0.5);
        assertTrue(clamped.equals(CelestialProjection.frame(-4, -0.5), EPSILON));
    }

    @Test
    void mapRoundTripsThroughCube() {
        for (double x = -3.95; x < 4; x += 0.1) {
            for (double z = -3.95; z < 4; z += 0.1) {
                Vector2d back = CelestialProjection.toMap(CelestialProjection.toCube(x, z));
                assertEquals(x, back.x, 1e-6, "x at " + x + ", " + z);
                assertEquals(z, back.y, 1e-6, "z at " + x + ", " + z);
            }
        }
    }

    @Test
    void cubeRoundTripsThroughMap() {
        for (double x = -1; x <= 1; x += 0.125) {
            for (double y = -1; y <= 1; y += 0.125) {
                for (Vector3d point : new Vector3d[]{
                        new Vector3d(x, y, -1), new Vector3d(x, y, 1), new Vector3d(1, x, y),
                        new Vector3d(-1, x, y), new Vector3d(x, 1, y), new Vector3d(x, -1, y)}) {
                    if (point.z == 1 && x == 0 && y == 0) continue; // The south face's centre is the whole map edge
                    Vector2d map = CelestialProjection.toMap(point);
                    assertVector(point, CelestialProjection.toCube(map.x, map.y));
                }
            }
        }
    }

    @Test
    void toMapTakesAnyDirection() {
        Vector2d onCube = CelestialProjection.toMap(new Vector3d(-1, 0.5, 0.25));
        Vector2d scaled = CelestialProjection.toMap(new Vector3d(-40, 20, 10));
        assertEquals(onCube.x, scaled.x, EPSILON);
        assertEquals(onCube.y, scaled.y, EPSILON);
    }

    @Test
    void upIsTheOutwardNormalOfTheFace() {
        for (double x = -3.95; x < 4; x += 0.1) {
            for (double z = -3.95; z < 4; z += 0.1) {
                Vector3d point = CelestialProjection.toCube(x, z);
                Vector3d up = CelestialProjection.frame(x, z).transform(new Vector3d(0, 1, 0));
                assertVector(faceNormal(point), up);
            }
        }
    }

    @Test
    void framesAreRotations() {
        for (double x = -3.95; x < 4; x += 0.1) {
            for (double z = -3.95; z < 4; z += 0.1) {
                Quaterniond frame = CelestialProjection.frame(x, z);
                Vector3d east = frame.transform(new Vector3d(1, 0, 0));
                Vector3d up = frame.transform(new Vector3d(0, 1, 0));
                Vector3d south = frame.transform(new Vector3d(0, 0, 1));
                assertEquals(1, east.length(), EPSILON);
                // Right-handed, as the map's frame is: east × up = south
                assertVector(south, east.cross(up, new Vector3d()));
            }
        }
    }

    @Test
    void movingAlongTheMapMovesTheSameWayOnTheCube() {
        // In the middle of each region, a small step along the map is a step the way the frame says, on the cube
        double[][] points = {{0.5, 0.2}, {2, 0.3}, {-2, 0.3}, {0.3, 2}, {0.3, -2}, {3.5, 0.2}, {-3.5, 0.2}, {0.2, 3.5},
                {0.2, -3.5}};
        double step = 1e-4;
        for (double[] point : points) {
            Quaterniond frame = CelestialProjection.frame(point[0], point[1]);
            Vector3d here = CelestialProjection.toCube(point[0], point[1]);

            boolean xOutward = Math.abs(point[0]) >= Math.abs(point[1]);
            double dx = xOutward ? Math.signum(point[0]) * step : 0;
            double dz = xOutward ? 0 : Math.signum(point[1]) * step;
            Vector3d moved = CelestialProjection.toCube(point[0] + dx, point[1] + dz).sub(here).normalize();
            Vector3d expected = frame.transform(new Vector3d(dx, 0, dz)).normalize();
            assertTrue(moved.dot(expected) > 0.99, "at " + point[0] + ", " + point[1] + ": " + moved + " vs " + expected);
        }
    }

    private static Vector3d faceNormal(Vector3dc point) {
        Vector3d abs = point.absolute(new Vector3d());
        int axis = abs.maxComponent();
        return new Vector3d().setComponent(axis, Math.signum(point.get(axis)));
    }

    private static void assertVector(Vector3dc expected, Vector3dc actual) {
        assertEquals(expected.x(), actual.x(), EPSILON, "x of " + actual);
        assertEquals(expected.y(), actual.y(), EPSILON, "y of " + actual);
        assertEquals(expected.z(), actual.z(), EPSILON, "z of " + actual);
    }
}
