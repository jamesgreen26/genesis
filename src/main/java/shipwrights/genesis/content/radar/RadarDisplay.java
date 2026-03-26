package shipwrights.genesis.content.radar;

import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;


import java.util.ArrayList;
import java.util.List;

public class RadarDisplay {

    public final int resolution;
    public final double[][] data;
    private static final double fov = 90;
    private final RadarScanner scanner = new RadarScanner(64, Math.PI / 4);


    public RadarDisplay(int resolution) {
        this.resolution = resolution;
        this.data = new double[resolution][resolution];
    }

    public void scan(Level level, Vector3dc camera, Vector3dc direction, Vector3dc up, List<Long> excludedShips) {
        clear();

        // Normalize direction and up vectors
        Vector3d directionNormalized = new Vector3d(direction).normalize();
        Vector3d upInput = new Vector3d(up).normalize();

        // Compute camera basis vectors from direction and up (using right-hand rule)
        // right = direction × up (perpendicular to both, pointing right)
        Vector3d right = new Vector3d(directionNormalized).cross(upInput).normalize();
        // Recompute up to ensure orthogonality: up = right × direction
        Vector3d upNormalized = new Vector3d(right).cross(directionNormalized).normalize();

        scanner.update(camera, directionNormalized, upNormalized, right);

        scanShips(level, camera, excludedShips);

        if (GenesisMod.isSpaceDimension(level)) {
            scanPlanets(level, camera);
            //scanAsteroidBelt(level, camera);
        }
    }

    private void scanShips(Level level, Vector3dc camera, List<Long> excludedShips) {
        VSGameUtilsKt.getShipObjectWorld(level).getAllShips().forEach(ship -> {
            if (!excludedShips.contains(ship.getId())) {
                scanBox(ship.getWorldAABB());
            }
        });
    }

    private void scanPlanets(Level level, Vector3dc camera) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        registry.forEach(body -> {
            double extent = body.getActualSize() / 2;
            Vector3dc pos = body.getPosition(GenesisMod.getTicks(level), registry);
            AABBdc box = new AABBd(pos.x() - extent, pos.y() - extent, pos.z() - extent, pos.x() + extent, pos.y() + extent, pos.z() + extent);
            scanBox(box);
        });
    }

    private void scanAsteroidBelt(Level level, Vector3dc camera) {
        // Torus parameters matching worldgen
        double majorRadius = 25_000; // distance from center to tube center
        double minorRadius = 470.0;    // radius of tube

        // Approximate torus as boxes arranged in a circle
        // Use more segments for smoother, continuous approximation
        int segments = 64;

        // Calculate arc length between segment centers
        double arcLength = (2.0 * Math.PI * majorRadius) / segments;

        // Box size needs to cover the arc length plus the tube radius for continuous coverage
        // Use Pythagorean theorem: box must reach from center to adjacent center's edge
        double boxRadialExtent = minorRadius + arcLength * 0.6; // 60% overlap ensures no gaps

        for (int i = 0; i < segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;

            // Position along the major radius
            double cx = Math.cos(angle) * majorRadius;
            double cz = Math.sin(angle) * majorRadius;

            // Create a box at this position with proper extent for continuous coverage
            AABBdc box = new org.joml.primitives.AABBd(
                cx - boxRadialExtent, -64, cz - boxRadialExtent,
                cx + boxRadialExtent, 256, cz + boxRadialExtent
            );

            scanBox(box);
        }
    }

    private void scanBox(AABBdc box) {
        Vector3dc[] corners = {
                new Vector3d(box.minX(), box.minY(), box.minZ()),
                new Vector3d(box.minX(), box.minY(), box.maxZ()),
                new Vector3d(box.minX(), box.maxY(), box.minZ()),
                new Vector3d(box.minX(), box.maxY(), box.maxZ()),
                new Vector3d(box.maxX(), box.minY(), box.minZ()),
                new Vector3d(box.maxX(), box.minY(), box.maxZ()),
                new Vector3d(box.maxX(), box.maxY(), box.minZ()),
                new Vector3d(box.maxX(), box.maxY(), box.maxZ())
        };

        List<RadarScanner.RadarScanResult> results = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            scanner.scanPoint(corners[i]).ifPresent(results::add);
        }
        if (results.isEmpty()) return;

        int maxX = -1;
        int minX = 9999;
        int maxY = -1;
        int minY = 9999;
        double depthSqr = 0;

        for (var result : results) {
            maxX = Math.max(maxX, result.x());
            minX = Math.min(minX, result.x());
            maxY = Math.max(maxY, result.y());
            minY = Math.min(minY, result.y());
            depthSqr = Math.max(depthSqr, result.distSquared());
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                writeDepth(x, y, Math.sqrt(depthSqr));
            }
        }
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
