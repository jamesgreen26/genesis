package shipwrights.genesis.client.shading;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.maplibre.earcut4j.Earcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.util.NoOpLogger;

import java.util.List;

public class ShadowRenderer {

    private static final Logger LOGGER = NoOpLogger.INSTANCE;
    private static final double Z_FIGHTING_EPSILON = 0.005;

    /**
     * Convert 2D plane coordinates to 3D local space coordinates.
     *
     * @param vertex2D 2D coordinates in plane space
     * @param plane The axis-aligned plane
     * @return 3D coordinates in local space
     */
    public static Vector3d convertPlaneToLocal3D(Vector2dc vertex2D, AAPlane plane) {
        Vector3i normal = plane.normal();
        double position = plane.position();

        // Map 2D coordinates to 3D based on plane normal
        if (Math.abs(normal.x()) == 1) {
            // X-normal plane: (y, z) mapping
            return new Vector3d(position, vertex2D.x(), vertex2D.y());
        } else if (Math.abs(normal.y()) == 1) {
            // Y-normal plane: (x, z) mapping
            return new Vector3d(vertex2D.x(), position, vertex2D.y());
        } else {
            // Z-normal plane: (x, y) mapping
            return new Vector3d(vertex2D.x(), vertex2D.y(), position);
        }
    }

    /**
     * Apply z-fighting offset by nudging vertex along plane normal.
     *
     * @param vertex3D The 3D vertex in local space
     * @param plane The axis-aligned plane
     * @param cubeHalfSize Half the size of the cube (for relative offset)
     * @return Offset vertex
     */
    public static Vector3d applyZFightingOffset(Vector3d vertex3D, AAPlane plane, double cubeHalfSize) {
        Vector3i normal = plane.normal();
        double offset = Z_FIGHTING_EPSILON * cubeHalfSize;

        // Offset in the direction of the normal
        return new Vector3d(
            vertex3D.x + normal.x() * offset,
            vertex3D.y + normal.y() * offset,
            vertex3D.z + normal.z() * offset
        );
    }

    /**
     * Triangulate a 2D polygon using Earcut4j.
     *
     * @param polygon List of 2D vertices
     * @return List of triangle indices (groups of 3)
     */
    public static List<Integer> triangulateShadowPolygon(List<Vector2dc> polygon) {
        if (polygon.size() < 3) {
            return List.of();
        }

        // Convert to flat double array [x1, y1, x2, y2, ...]
        double[] coords = new double[polygon.size() * 2];
        for (int i = 0; i < polygon.size(); i++) {
            coords[i * 2] = polygon.get(i).x();
            coords[i * 2 + 1] = polygon.get(i).y();
        }

        // Triangulate
        try {
            return Earcut.earcut(coords, null, 2);
        } catch (Exception e) {
            LOGGER.warn("Failed to triangulate shadow polygon: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Compute the edge-softness width for a shadow in local space.
     *
     * This must be calculated CPU-side so that adjacent faces sharing the same
     * shadow source use an identical value, avoiding visual discontinuities at
     * cube edges.  The 2D polygon lives in plane space whose axes map directly
     * to two of the three world-space axes, so 2D distances equal world-space
     * distances after the planet matrix (rotation + translation, no scale).
     *
     * @param shadow The face shadow
     * @return Edge width to pass as ShadowEdgeWidth uniform
     */
    public static float computeEdgeWidth(FaceShadow shadow) {
        List<Vector2dc> polygon = shadow.polygon();
        if (polygon.isEmpty()) return 0.0001f;

        double cx = 0, cy = 0;
        for (Vector2dc v : polygon) {
            cx += v.x();
            cy += v.y();
        }
        cx /= polygon.size();
        cy /= polygon.size();

        double maxDist = 0;
        for (Vector2dc v : polygon) {
            double dx = v.x() - cx;
            double dy = v.y() - cy;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dy * dy));
        }

        return (float) Math.max(maxDist * 0.25, 0.0001);
    }

    /**
     * Render a single shadow onto a vertex buffer.
     *
     * @param shadow The shadow to render
     * @param matrix Transformation matrix
     * @param buffer Vertex consumer
     * @param cubeHalfSize Half the size of the cube
     */
    public static void renderShadow(FaceShadow shadow, Matrix4f matrix, VertexConsumer buffer, double cubeHalfSize) {
        List<Vector2dc> polygon = shadow.polygon();
        AAPlane plane = shadow.plane();

        LOGGER.info("renderShadow: Processing shadow with {} vertices", polygon.size());

        // Triangulate
        List<Integer> indices = triangulateShadowPolygon(polygon);
        if (indices.isEmpty() || indices.size() % 3 != 0) {
            LOGGER.warn("Triangulation failed or produced invalid result. Indices: {}", indices.size());
            return; // Invalid triangulation
        }

        LOGGER.info("Triangulation successful: {} triangles ({} indices)", indices.size() / 3, indices.size());

        // Convert 2D vertices to 3D with offset.
        // Vertices that sit on a face boundary (±cubeHalfSize) are extended slightly
        // beyond the boundary so that adjacent face shadows overlap and close the seam
        // gap introduced by the z-fighting offset.
        double seamExt = Z_FIGHTING_EPSILON * cubeHalfSize;
        double eps = cubeHalfSize * 1e-4;
        Vector3d[] vertices3D = new Vector3d[polygon.size()];
        for (int i = 0; i < polygon.size(); i++) {
            Vector2dc v2d = polygon.get(i);
            double vx = v2d.x();
            double vy = v2d.y();
            if      (Math.abs(vx - cubeHalfSize) < eps) vx += seamExt;
            else if (Math.abs(vx + cubeHalfSize) < eps) vx -= seamExt;
            if      (Math.abs(vy - cubeHalfSize) < eps) vy += seamExt;
            else if (Math.abs(vy + cubeHalfSize) < eps) vy -= seamExt;
            Vector3d v3d = convertPlaneToLocal3D(new Vector2d(vx, vy), plane);
            vertices3D[i] = applyZFightingOffset(v3d, plane, cubeHalfSize);
            LOGGER.info("Vertex {}: 2D({}, {}) -> 3D({}, {}, {})",
                    i, polygon.get(i).x(), polygon.get(i).y(),
                    vertices3D[i].x, vertices3D[i].y, vertices3D[i].z);
        }

        int r = 0, g = 0, b = 0, a = 176;

        LOGGER.info("Rendering {} triangles with color rgba({}, {}, {}, {})", indices.size() / 3, r, g, b, a);

        // Check if we need to reverse winding order for this plane
        // Y-normal and X-normal planes need reversed winding due to coordinate mapping
        boolean reverseWinding = plane.normal().x() == -1 || plane.normal().y() == 1 || plane.normal().z() == -1;

        // Render triangles
        int triangleCount = 0;
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            // Validate indices
            if (i0 >= vertices3D.length || i1 >= vertices3D.length || i2 >= vertices3D.length) {
                LOGGER.warn("Invalid triangle indices: {}, {}, {} (max: {})", i0, i1, i2, vertices3D.length - 1);
                continue; // Skip invalid triangle
            }

            Vector3d v0 = vertices3D[i0];
            Vector3d v1 = vertices3D[i1];
            Vector3d v2 = vertices3D[i2];

            // Add vertices - reverse order for Y and X normal planes
            if (reverseWinding) {
                buffer.vertex(matrix, (float)v0.x, (float)v0.y, (float)v0.z).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(r, g, b, a).endVertex();
            } else {
                buffer.vertex(matrix, (float)v0.x, (float)v0.y, (float)v0.z).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(r, g, b, a).endVertex();
            }
            triangleCount++;
        }

        LOGGER.info("Successfully rendered {} triangles", triangleCount);
    }
}
