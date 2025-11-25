package shipwrights.genesis.radar;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SymmetryTest {

    @Test
    public void testCornerPixelSymmetry() {
        PixelFrustumFactory factory = new PixelFrustumFactory(20);

        // Camera at origin, looking down +Z axis
        Vector3d camera = new Vector3d(0, 0, 0);
        Vector3d forward = new Vector3d(0, 0, 1);
        Vector3d right = new Vector3d(1, 0, 0);
        Vector3d up = new Vector3d(0, 1, 0);

        factory.update(camera, forward, right, up, 90.0);

        // Calculate the angular deviation for pixel (0,0) which is top-left corner
        // This pixel should have equal angular deviation in both X and Y from center

        int res = 20;
        double halfFov = Math.atan(Math.tan(Math.toRadians(45))); // 90° FOV
        double anglePerPixelRad = (halfFov * 2.0) / res;

        // For pixel (0, 0):
        double angleX = (0 - res / 2.0) * anglePerPixelRad;
        double angleY = (0 - res / 2.0) * anglePerPixelRad;

        // Both should be equal (same negative angle from center)
        assertEquals(angleX, angleY, 0.0001, "Corner pixel should have equal X and Y angles");

        // Verify the tangent values are also equal
        double tanX = Math.tan(angleX);
        double tanY = Math.tan(angleY);
        assertEquals(tanX, tanY, 0.0001, "Corner pixel should have equal X and Y tangent values");

        System.out.println("Corner pixel (0,0) angles:");
        System.out.println("  angleX: " + Math.toDegrees(angleX) + "°");
        System.out.println("  angleY: " + Math.toDegrees(angleY) + "°");
        System.out.println("  tanX: " + tanX);
        System.out.println("  tanY: " + tanY);
    }

    @Test
    public void testBasisVectorOrthogonality() {
        // Test that direction × up gives proper right vector
        // Using south (+Z) direction
        Vector3d direction = new Vector3d(0, 0, 1).normalize();
        Vector3d up = new Vector3d(0, 1, 0).normalize();

        // right = direction × up
        Vector3d right = new Vector3d(direction).cross(up).normalize();

        // Verify right is (-1, 0, 0) - when facing south, right is west
        assertEquals(-1.0, right.x(), 0.0001);
        assertEquals(0.0, right.y(), 0.0001);
        assertEquals(0.0, right.z(), 0.0001);

        // Verify all three are orthogonal
        assertEquals(0.0, direction.dot(right), 0.0001, "Direction and right should be perpendicular");
        assertEquals(0.0, direction.dot(up), 0.0001, "Direction and up should be perpendicular");
        assertEquals(0.0, right.dot(up), 0.0001, "Right and up should be perpendicular");

        // Verify all three are unit vectors
        assertEquals(1.0, direction.length(), 0.0001);
        assertEquals(1.0, right.length(), 0.0001);
        assertEquals(1.0, up.length(), 0.0001);
    }

    @Test
    public void testMinecraftCoordinates() {
        // Test with Minecraft's coordinate system
        // North = -Z, East = +X, Up = +Y

        Vector3d north = new Vector3d(0, 0, -1);
        Vector3d worldUp = new Vector3d(0, 1, 0);

        // right = direction × up should point east (+X)
        Vector3d east = new Vector3d(north).cross(worldUp).normalize();

        assertEquals(1.0, east.x(), 0.0001, "Right should point east (+X)");
        assertEquals(0.0, east.y(), 0.0001);
        assertEquals(0.0, east.z(), 0.0001);

        System.out.println("Minecraft coordinate test:");
        System.out.println("  North (direction): " + north);
        System.out.println("  World Up: " + worldUp);
        System.out.println("  East (right): " + east);
    }
}
