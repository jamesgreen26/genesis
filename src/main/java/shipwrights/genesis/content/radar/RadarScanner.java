package shipwrights.genesis.content.radar;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Optional;
import java.util.function.BiConsumer;

public class RadarScanner {
    public final Vector3d[][] rays;
    public final double fov;
    private final int resolution;
    private double minDot = 0.0;
    private Vector3dc cameraPos = new Vector3d();

    public RadarScanner(int resolution, double fov) {
        this.rays = new Vector3d[resolution][resolution];
        this.fov = fov;
        this.resolution = resolution;

        forEachRay((x, y) -> {
            this.rays[x][y] = new Vector3d(0, 0, 1);
        });
    }

    void update(Vector3dc cameraPos, Vector3dc forward, Vector3dc up, Vector3dc right) {
        this.cameraPos = cameraPos;
        this.minDot = Math.cos((fov / resolution) * Math.sqrt(2.0) / 2.0);

        forEachRay((x, y) -> {
            Vector3d it = this.rays[x][y];
            it.set(forward);
            double u = ((x + 0.5) / resolution) - 0.5;
            double v = ((y + 0.5) / resolution) - 0.5;

            double rotY = u * fov;
            double rotX = v * fov;

            it.rotateAxis(rotY, up.x(), up.y(), up.z());
            it.rotateAxis(rotX, right.x(), right.y(), right.z());
        });
    }

    Optional<RadarScanResult> scanPoint(Vector3dc point) {
        Vector3d ray = point.sub(cameraPos, new Vector3d());
        double maxDot = -1;
        int resX = 0;
        int resY = 0;
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                double dot = rays[x][y].dot(ray);
                if (dot > maxDot) {
                    maxDot = dot;
                    resX = x;
                    resY = y;
                }
            }
        }

        if (maxDot > minDot) {
            return Optional.of(new RadarScanResult(resX, resY, cameraPos.distanceSquared(point)));
        } else {
            return Optional.empty();
        }
    }

    private void forEachRay(BiConsumer<Integer, Integer> callback) {
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                callback.accept(x, y);
            }
        }
    }

    public record RadarScanResult(int x, int y, double distSquared) { }
}
