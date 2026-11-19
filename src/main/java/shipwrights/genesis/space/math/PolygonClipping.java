package shipwrights.genesis.space.math;

import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public class PolygonClipping {
    /**
     * Computes the 2D cross product (signed area) of triangle (a,b,c).
     * Positive if a->b->c is a left turn, negative if right turn, zero if collinear.
     *
     * @param a first vertex
     * @param b second vertex
     * @param c third vertex
     * @return signed area (cross product)
     */
    public static double cross(Vector2d a, Vector2d b, Vector2d c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    /**
     * Computes the convex hull of a set of 2D points using Graham scan algorithm.
     * Returns vertices in counter-clockwise order.
     *
     * @param points input list of points
     * @return new list containing convex hull vertices in CCW order
     */
    public static List<Vector2d> convexHull(List<Vector2d> points) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Find the bottom-most point (or leftmost if tie)
        Vector2d pivot = points.get(0);
        for (Vector2d p : points) {
            if (p.y < pivot.y || (p.y == pivot.y && p.x < pivot.x)) {
                pivot = p;
            }
        }

        // Sort points by polar angle with respect to pivot
        final Vector2d finalPivot = pivot;
        List<Vector2d> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> {
            boolean aIsPivot = a.equals(finalPivot);
            boolean bIsPivot = b.equals(finalPivot);
            if (aIsPivot && bIsPivot) return 0;
            if (aIsPivot) return -1;
            if (bIsPivot) return 1;

            double angleA = Math.atan2(a.y - finalPivot.y, a.x - finalPivot.x);
            double angleB = Math.atan2(b.y - finalPivot.y, b.x - finalPivot.x);

            int angleCompare = Double.compare(angleA, angleB);
            if (angleCompare != 0) return angleCompare;

            // If angles are equal, closer point comes first
            double distA = finalPivot.distanceSquared(a);
            double distB = finalPivot.distanceSquared(b);
            return Double.compare(distA, distB);
        });

        // Build the hull using Graham scan
        List<Vector2d> hull = new ArrayList<>();
        for (Vector2d p : sorted) {
            // Remove points that would create a right turn
            while (hull.size() >= 2) {
                Vector2d top = hull.get(hull.size() - 1);
                Vector2d secondTop = hull.get(hull.size() - 2);
                if (cross(secondTop, top, p) <= 0) {
                    hull.remove(hull.size() - 1);
                } else {
                    break;
                }
            }
            hull.add(p);
        }

        return hull;
    }
}
