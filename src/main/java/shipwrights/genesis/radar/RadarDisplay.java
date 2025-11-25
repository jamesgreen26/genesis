package shipwrights.genesis.radar;

import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;

import java.util.List;

public class RadarDisplay {

    public final int resolution;
    public final double[][] data;
    private final PixelFrustumFactory frustumFactory;
    private static final double fov = 90;

    // Debug data (stored after scan for visualization)
    public Vector3dc debugCamera;
    public Vector3dc debugDirection;
    public Vector3dc debugUp;

    public RadarDisplay(int resolution) {
        this.resolution = resolution;
        this.data = new double[resolution][resolution];
        this.frustumFactory = new PixelFrustumFactory(resolution);
    }

    public PixelFrustumFactory getFrustumFactory() {
        return frustumFactory;
    }


    public void scan(Level level, Vector3dc camera, Vector3dc direction, Vector3dc up, List<Long> excludedShips) {
        clear();

        // Store debug data
        debugCamera = camera;
        debugDirection = direction;
        debugUp = up;

        // Normalize direction and up vectors
        Vector3d directionNormalized = new Vector3d(direction).normalize();
        Vector3d upInput = new Vector3d(up).normalize();

        // Compute camera basis vectors from direction and up (using right-hand rule)
        // right = direction × up (perpendicular to both, pointing right)
        Vector3d right = new Vector3d(directionNormalized).cross(upInput).normalize();
        // Recompute up to ensure orthogonality: up = right × direction
        Vector3d upNormalized = new Vector3d(right).cross(directionNormalized).normalize();

        // Update all frustums with new view parameters
        frustumFactory.update(camera, directionNormalized, right, upNormalized, fov);

        scanShips(level, camera, excludedShips);

        if (GenesisMod.isSpaceDimension(level)) {
            scanPlanets(level, camera);
//            scanAsteroidBelt(level, camera);
        }
    }

    private void scanShips(Level level, Vector3dc camera, List<Long> excludedShips) {
        VSGameUtilsKt.getShipObjectWorld(level).getAllShips().forEach(ship -> {
            if (!excludedShips.contains(ship.getId())) {
                scanBox(ship.getWorldAABB(), camera);
            }
        });
    }

    private void scanPlanets(Level level, Vector3dc camera) {
        GenesisMod.planets.forEach(planetData -> {
            double extent = planetData.getActualSize() / 2;
            Vector3dc pos = planetData.getCurrentPos(level.getGameTime());
            AABBdc box = new AABBd(pos.x() - extent, pos.y() - extent, pos.z() - extent, pos.x() + extent, pos.y() + extent, pos.z() + extent);
            scanBox(box, camera);
        });
    }

    private void scanAsteroidBelt(Level level, Vector3dc camera) {
        // Torus parameters matching worldgen
        double majorRadius = GenesisMod.earthDist * 1.6667; // distance from center to tube center
        double minorRadius = 470.0;    // radius of tube
        double centerY = -100.0;       // Y offset

        // Approximate torus as boxes arranged in a circle
        int segments = 16; // number of boxes around the ring
        for (int i = 0; i < segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;

            // Position along the major radius
            double cx = Math.cos(angle) * majorRadius;
            double cz = Math.sin(angle) * majorRadius;

            // Create a box at this position with minor radius extent
            double boxSize = minorRadius * 1.5; // slightly larger for overlap
            AABBdc box = new org.joml.primitives.AABBd(
                cx - boxSize, centerY - minorRadius, cz - boxSize,
                cx + boxSize, centerY + minorRadius, cz + boxSize
            );

            scanBox(box, camera);
        }
    }

    private void scanBox(AABBdc box, Vector3dc camera) {
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {

                PixelFrustum fr = frustumFactory.getFrustum(x, y);

                if (!fr.intersectsAabb(box))
                    continue;

                // depth approximation = distance to closest point of AABB
                double depth = computeDepth(box, camera);

                writeDepth(x, y, depth);
            }
        }
    }

    private double computeDepth(AABBdc box, Vector3dc camera) {
        double cx = clamp(camera.x(), box.minX(), box.maxX());
        double cy = clamp(camera.y(), box.minY(), box.maxY());
        double cz = clamp(camera.z(), box.minZ(), box.maxZ());
        return camera.distance(cx, cy, cz);
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public void writeDepth(int x, int y, double depth) {
        double existing = data[x][y];
        if (existing == 0 || depth < existing) {
            data[x][y] = depth;
        }
    }

    private void clear() {
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                data[x][y] = 0;
            }
        }
    }
}
