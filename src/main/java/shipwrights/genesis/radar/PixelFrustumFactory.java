package shipwrights.genesis.radar;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PixelFrustumFactory {

    private final int res;
    private final PixelFrustum[][] frustums;

    private Vector3dc C;
    private Vector3dc F;
    private Vector3dc R;
    private Vector3dc U;
    private double tanHalfFov;

    public PixelFrustumFactory(int res) {
        this.res = res;
        this.frustums = new PixelFrustum[res][res];

        // Initialize all frustums with placeholder planes
        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                Vector3d dummyNormal = new Vector3d(1, 0, 0);
                Vector3d dummyPoint = new Vector3d(0, 0, 0);
                PixelFrustum.Plane left = new PixelFrustum.Plane(dummyNormal, dummyPoint);
                PixelFrustum.Plane right = new PixelFrustum.Plane(dummyNormal, dummyPoint);
                PixelFrustum.Plane top = new PixelFrustum.Plane(dummyNormal, dummyPoint);
                PixelFrustum.Plane bottom = new PixelFrustum.Plane(dummyNormal, dummyPoint);
                frustums[x][y] = new PixelFrustum(left, right, top, bottom);
            }
        }
    }

    public void update(Vector3dc camera, Vector3dc forward, Vector3dc right, Vector3dc up, double fovDeg) {
        this.C = camera;
        this.F = forward;
        this.R = right;
        this.U = up;
        this.tanHalfFov = Math.tan(Math.toRadians(fovDeg) / 2.0);

        // Recalculate all frustums
        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                updateFrustum(x, y);
            }
        }
    }

    public PixelFrustum getFrustum(int px, int py) {
        return frustums[px][py];
    }

    // Debug getters for rendering frustums
    public Vector3dc getCamera() { return C; }
    public Vector3dc getForward() { return F; }
    public Vector3dc getRight() { return R; }
    public Vector3dc getUp() { return U; }
    public double getTanHalfFov() { return tanHalfFov; }

    private void updateFrustum(int px, int py) {
        // Calculate equal angular spacing for each pixel
        // Convert tanHalfFov back to angle, then divide FOV equally among all pixels
        double halfFov = Math.atan(tanHalfFov);
        double anglePerPixelRad = (halfFov * 2.0) / res;

        // Angular boundaries for this pixel (centered at 0)
        double angleX0 = (px - res / 2.0) * anglePerPixelRad;
        double angleX1 = (px + 1 - res / 2.0) * anglePerPixelRad;
        double angleY0 = (py - res / 2.0) * anglePerPixelRad;
        double angleY1 = (py + 1 - res / 2.0) * anglePerPixelRad;

        // Convert angles to tangent values for ray construction
        double tanX0 = Math.tan(angleX0);
        double tanX1 = Math.tan(angleX1);
        double tanY0 = Math.tan(angleY0);
        double tanY1 = Math.tan(angleY1);

        // corner rays using tangent values
        Vector3d d00 = cornerRay(tanX0, tanY0);
        Vector3d d10 = cornerRay(tanX1, tanY0);
        Vector3d d01 = cornerRay(tanX0, tanY1);
        Vector3d d11 = cornerRay(tanX1, tanY1);

        // planes: inward-pointing normals for frustum culling
        PixelFrustum.Plane left   = planeFromRays(d01, d00);
        PixelFrustum.Plane right  = planeFromRays(d10, d11);
        PixelFrustum.Plane top    = planeFromRays(d00, d10);
        PixelFrustum.Plane bottom = planeFromRays(d11, d01);

        frustums[px][py].update(left, right, top, bottom);
    }

    private Vector3d cornerRay(double tanX, double tanY) {
        // Construct ray directly from tangent values
        return new Vector3d(F)
                .add(new Vector3d(R).mul(tanX))
                .add(new Vector3d(U).mul(tanY))
                .normalize();
    }

    private PixelFrustum.Plane planeFromRays(Vector3d a, Vector3d b) {
        // plane normal = a × b (inward-pointing for frustum culling)
        Vector3d n = new Vector3d(a).cross(b).normalize();
        return new PixelFrustum.Plane(n, C);
    }
}
