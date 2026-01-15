package shipwrights.genesis.client.shading;

import org.joml.Vector2d;
import org.joml.Vector2dc;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.PolygonClipping;

import java.util.ArrayList;
import java.util.List;

public record FaceShadow(AAPlane plane, List<Vector2dc> polygon) {

    public FaceShadow merge(FaceShadow other) {
        if (!other.plane.equals(plane)) {
            throw new IllegalArgumentException("Cannot merge faces on different planes");
        }

        // Collect all vertices from both polygons
        List<Vector2d> allVertices = collectAllVertices(this.polygon, other.polygon);

        // Compute convex hull to get the merged boundary
        List<Vector2d> hull = PolygonClipping.convexHull(allVertices);

        // Prune any collinear points for a cleaner result
        hull = PolygonClipping.pruneCollinear(hull, 1e-12);

        // Convert back to immutable Vector2dc
        List<Vector2dc> mergedPolygon = convertToImmutable(hull);

        return new FaceShadow(plane, mergedPolygon);
    }

    /**
     * Collects all vertices from both polygons into a single mutable list.
     *
     * @param polygon1 first polygon vertices
     * @param polygon2 second polygon vertices
     * @return combined list of mutable Vector2d points
     */
    private static List<Vector2d> collectAllVertices(List<Vector2dc> polygon1, List<Vector2dc> polygon2) {
        List<Vector2d> result = new ArrayList<>(polygon1.size() + polygon2.size());

        for (Vector2dc v : polygon1) {
            result.add(new Vector2d(v));
        }

        for (Vector2dc v : polygon2) {
            result.add(new Vector2d(v));
        }

        return result;
    }

    /**
     * Converts a list of mutable Vector2d to immutable Vector2dc.
     *
     * @param vertices list of mutable points
     * @return immutable list of immutable points
     */
    private static List<Vector2dc> convertToImmutable(List<Vector2d> vertices) {
        List<Vector2dc> result = new ArrayList<>(vertices.size());
        result.addAll(vertices);
        return List.copyOf(result);
    }
}

