package shipwrights.genesis.radar;

import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PixelFrustumTest {

    @Test
    public void testPlaneCreation() {
        // Test plane at origin facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        assertEquals(1.0, plane.normal.x(), 0.001);
        assertEquals(0.0, plane.normal.y(), 0.001);
        assertEquals(0.0, plane.normal.z(), 0.001);
        assertEquals(0.0, plane.d, 0.001); // plane eq: x + d = 0, at origin d should be 0
    }

    @Test
    public void testPlaneAtOffset() {
        // Test plane at x=5 facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(5, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        assertEquals(1.0, plane.normal.x(), 0.001);
        assertEquals(-5.0, plane.d, 0.001); // plane eq: x - 5 = 0
    }

    @Test
    public void testPlaneIsOutside_CompletelyOutside() {
        // Plane at origin facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        // AABB completely in negative X (behind plane)
        AABBdc box = new AABBd(-10, -1, -1, -5, 1, 1);
        assertTrue(plane.isOutside(box), "Box in negative X should be outside plane facing +X");
    }

    @Test
    public void testPlaneIsOutside_CompletelyInside() {
        // Plane at origin facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        // AABB completely in positive X (in front of plane)
        AABBdc box = new AABBd(5, -1, -1, 10, 1, 1);
        assertFalse(plane.isOutside(box), "Box in positive X should be inside plane facing +X");
    }

    @Test
    public void testPlaneIsOutside_Intersecting() {
        // Plane at origin facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        // AABB straddling the plane
        AABBdc box = new AABBd(-2, -1, -1, 3, 1, 1);
        assertFalse(plane.isOutside(box), "Box straddling plane should not be outside");
    }

    @Test
    public void testPlaneIsOutside_TouchingPlane() {
        // Plane at origin facing +X
        Vector3d normal = new Vector3d(1, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal, point);

        // AABB just touching the plane at origin
        AABBdc box = new AABBd(0, -1, -1, 5, 1, 1);
        assertFalse(plane.isOutside(box), "Box touching plane should not be outside");
    }

    @Test
    public void testFrustumIntersectsAabb_CompletelyInside() {
        // Create a simple frustum with 4 planes forming a box
        Vector3d camera = new Vector3d(0, 0, 0);

        // Left plane (facing right)
        PixelFrustum.Plane left = new PixelFrustum.Plane(new Vector3d(1, 0, 0), camera);
        // Right plane (facing left)
        PixelFrustum.Plane right = new PixelFrustum.Plane(new Vector3d(-1, 0, 0), new Vector3d(10, 0, 0));
        // Bottom plane (facing up)
        PixelFrustum.Plane bottom = new PixelFrustum.Plane(new Vector3d(0, 1, 0), camera);
        // Top plane (facing down)
        PixelFrustum.Plane top = new PixelFrustum.Plane(new Vector3d(0, -1, 0), new Vector3d(0, 10, 0));

        PixelFrustum frustum = new PixelFrustum(left, right, top, bottom);

        // AABB inside the frustum bounds (0-10 in X, 0-10 in Y)
        AABBdc box = new AABBd(2, 2, -5, 8, 8, 5);
        assertTrue(frustum.intersectsAabb(box), "Box completely inside frustum should intersect");
    }

    @Test
    public void testFrustumIntersectsAabb_CompletelyOutside() {
        // Create a simple frustum with 4 planes forming a box
        Vector3d camera = new Vector3d(0, 0, 0);

        // Frustum bounds: 0-10 in X, 0-10 in Y
        PixelFrustum.Plane left = new PixelFrustum.Plane(new Vector3d(1, 0, 0), camera);
        PixelFrustum.Plane right = new PixelFrustum.Plane(new Vector3d(-1, 0, 0), new Vector3d(10, 0, 0));
        PixelFrustum.Plane bottom = new PixelFrustum.Plane(new Vector3d(0, 1, 0), camera);
        PixelFrustum.Plane top = new PixelFrustum.Plane(new Vector3d(0, -1, 0), new Vector3d(0, 10, 0));

        PixelFrustum frustum = new PixelFrustum(left, right, top, bottom);

        // AABB outside the frustum (negative X)
        AABBdc box = new AABBd(-10, 2, -5, -5, 8, 5);
        assertFalse(frustum.intersectsAabb(box), "Box outside frustum should not intersect");
    }

    @Test
    public void testFrustumIntersectsAabb_PartiallyInside() {
        // Create a simple frustum with 4 planes forming a box
        Vector3d camera = new Vector3d(0, 0, 0);

        // Frustum bounds: 0-10 in X, 0-10 in Y
        PixelFrustum.Plane left = new PixelFrustum.Plane(new Vector3d(1, 0, 0), camera);
        PixelFrustum.Plane right = new PixelFrustum.Plane(new Vector3d(-1, 0, 0), new Vector3d(10, 0, 0));
        PixelFrustum.Plane bottom = new PixelFrustum.Plane(new Vector3d(0, 1, 0), camera);
        PixelFrustum.Plane top = new PixelFrustum.Plane(new Vector3d(0, -1, 0), new Vector3d(0, 10, 0));

        PixelFrustum frustum = new PixelFrustum(left, right, top, bottom);

        // AABB partially inside (straddles left boundary)
        AABBdc box = new AABBd(-2, 2, -5, 5, 8, 5);
        assertTrue(frustum.intersectsAabb(box), "Box partially inside frustum should intersect");
    }

    @Test
    public void testPlaneUpdate() {
        // Create a plane
        Vector3d normal1 = new Vector3d(1, 0, 0);
        Vector3d point1 = new Vector3d(0, 0, 0);
        PixelFrustum.Plane plane = new PixelFrustum.Plane(normal1, point1);

        assertEquals(0.0, plane.d, 0.001);

        // Update the plane
        Vector3d normal2 = new Vector3d(0, 1, 0);
        Vector3d point2 = new Vector3d(0, 5, 0);
        plane.update(normal2, point2);

        assertEquals(0.0, plane.normal.x(), 0.001);
        assertEquals(1.0, plane.normal.y(), 0.001);
        assertEquals(0.0, plane.normal.z(), 0.001);
        assertEquals(-5.0, plane.d, 0.001);
    }

    @Test
    public void testFrustumUpdate() {
        // Create initial frustum
        Vector3d camera = new Vector3d(0, 0, 0);
        PixelFrustum.Plane left = new PixelFrustum.Plane(new Vector3d(1, 0, 0), camera);
        PixelFrustum.Plane right = new PixelFrustum.Plane(new Vector3d(-1, 0, 0), new Vector3d(10, 0, 0));
        PixelFrustum.Plane top = new PixelFrustum.Plane(new Vector3d(0, 1, 0), camera);
        PixelFrustum.Plane bottom = new PixelFrustum.Plane(new Vector3d(0, -1, 0), new Vector3d(0, 10, 0));

        PixelFrustum frustum = new PixelFrustum(left, right, top, bottom);

        // Create new planes
        PixelFrustum.Plane newLeft = new PixelFrustum.Plane(new Vector3d(0.707, 0.707, 0), camera);
        PixelFrustum.Plane newRight = new PixelFrustum.Plane(new Vector3d(-0.707, -0.707, 0), new Vector3d(10, 10, 0));
        PixelFrustum.Plane newTop = new PixelFrustum.Plane(new Vector3d(0, 1, 0), new Vector3d(0, 5, 0));
        PixelFrustum.Plane newBottom = new PixelFrustum.Plane(new Vector3d(0, -1, 0), new Vector3d(0, -5, 0));

        // Update frustum
        frustum.update(newLeft, newRight, newTop, newBottom);

        // Verify the update (check one of the planes)
        AABBdc testBox = new AABBd(0, 6, -1, 1, 7, 1);
        // This box should be outside the new top plane at y=5
        assertFalse(frustum.intersectsAabb(testBox), "Box above updated top plane should not intersect");
    }
}
