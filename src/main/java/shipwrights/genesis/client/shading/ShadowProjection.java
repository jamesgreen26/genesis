package shipwrights.genesis.client.shading;

import com.mojang.logging.LogUtils;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBdc;
import org.slf4j.Logger;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.math.Occlusion;
import shipwrights.genesis.math.PolygonClipping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShadowProjection {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static List<FaceShadow> computeShadows(
            OBB self,
            List<OBB> others,
            Vector3dc referencePoint
    ) {
        List<OBB> occluders = Occlusion.getPotentiallyOccluding(self, others, referencePoint);

        List<AAPlane> facingPlanes = PlanetShading.getFacingPlanes(self, referencePoint);

        Map<AAPlane, List<List<Vector2d>>> perPlane =
                accumulateProjectedPolygons(self, occluders, facingPlanes, referencePoint);

        return finalizeShadowPolygons(perPlane);
    }

    static Map<AAPlane, List<List<Vector2d>>> accumulateProjectedPolygons(
            OBB self,
            List<OBB> occluders,
            List<AAPlane> facingPlanes,
            Vector3dc referencePoint
    ) {
        Map<AAPlane, List<List<Vector2d>>> result = new HashMap<>();

        for (OBB occluder : occluders) {
            Map<AAPlane, List<Vector2dc>> projections =
                    PlanetShading.projectOBB2OntoOBB1Planes(
                            self, occluder, referencePoint
                    );

            for (AAPlane plane : facingPlanes) {
                List<Vector2dc> poly = projections.get(plane);
                if (poly == null || poly.size() < 3) continue;

                List<Vector2d> clipped =
                        projectAndClip(self, plane, poly);

                if (clipped.size() < 3) continue;

                result
                        .computeIfAbsent(plane, p -> new ArrayList<>())
                        .add(clipped);
            }
        }
        return result;
    }

    static List<Vector2d> projectAndClip(
            OBB self,
            AAPlane plane,
            List<Vector2dc> polygon
    ) {
        List<Vector2d> poly2d = polygon.stream()
                .map(v -> new Vector2d(v.x(), v.y()))
                .toList();

        // The projected corners arrive in axis-enumeration order (not polygon order).
        // Sort them into a valid CCW convex polygon before clipping; Sutherland-Hodgman
        // requires a properly ordered polygon or it will clip the wrong "edges".
        poly2d = PolygonClipping.angleSort(poly2d);

        List<Vector2d> clipped =
                clipToPlaneBounds(self, plane, poly2d);

        if (clipped.size() < 3) return List.of();

        return clipped;
    }

    static List<Vector2d> clipToPlaneBounds(
            OBB self,
            AAPlane plane,
            List<Vector2d> polygon
    ) {
        AABBdc aabb = self.localAabb();
        Vector3i n = plane.normal();

        if (Math.abs(n.x()) == 1) {
            return PolygonClipping.clipPolygonToRect(
                    polygon,
                    aabb.minY(), aabb.minZ(),
                    aabb.maxY(), aabb.maxZ()
            );
        }
        if (Math.abs(n.y()) == 1) {
            return PolygonClipping.clipPolygonToRect(
                    polygon,
                    aabb.minX(), aabb.minZ(),
                    aabb.maxX(), aabb.maxZ()
            );
        }
        return PolygonClipping.clipPolygonToRect(
                polygon,
                aabb.minX(), aabb.minY(),
                aabb.maxX(), aabb.maxY()
        );
    }

    static List<FaceShadow> finalizeShadowPolygons(
            Map<AAPlane, List<List<Vector2d>>> perPlane
    ) {
        List<FaceShadow> result = new ArrayList<>();

        for (Map.Entry<AAPlane, List<List<Vector2d>>> e : perPlane.entrySet()) {
            List<List<Vector2d>> polygons = e.getValue();

            if (polygons.isEmpty()) continue;

            // Collect all vertices from all polygons on this plane
            List<Vector2d> allVertices = new ArrayList<>();
            for (List<Vector2d> poly : polygons) {
                allVertices.addAll(poly);
            }

            if (allVertices.size() < 3) continue;

            // Use convex hull to merge all shadow regions on this plane
            List<Vector2d> merged = PolygonClipping.convexHull(allVertices);

            if (merged.size() >= 3) {
                result.add(new FaceShadow(e.getKey(), List.copyOf(merged)));
            }
        }
        return List.copyOf(result);
    }
}
