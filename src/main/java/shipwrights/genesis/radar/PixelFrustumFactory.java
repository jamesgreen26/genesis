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

    private void updateFrustum(int px, int py) {
        // pixel region in NDC
        double x0 = (double) px / res * 2.0 - 1.0;
        double x1 = (double)(px+1) / res * 2.0 - 1.0;
        double y0 = (double) py / res * 2.0 - 1.0;
        double y1 = (double)(py+1) / res * 2.0 - 1.0;

        // corner rays
        Vector3d d00 = cornerRay(x0, y0);
        Vector3d d10 = cornerRay(x1, y0);
        Vector3d d01 = cornerRay(x0, y1);
        Vector3d d11 = cornerRay(x1, y1);

        // planes: outward normals
        PixelFrustum.Plane left   = planeFromRays(d01, d00);
        PixelFrustum.Plane right  = planeFromRays(d10, d11);
        PixelFrustum.Plane top    = planeFromRays(d00, d10);
        PixelFrustum.Plane bottom = planeFromRays(d11, d01);

        frustums[px][py].update(left, right, top, bottom);
    }

    private Vector3d cornerRay(double nx, double ny) {
        return new Vector3d(F)
                .add(new Vector3d(R).mul(nx * tanHalfFov))
                .add(new Vector3d(U).mul(ny * tanHalfFov))
                .normalize();
    }

    private PixelFrustum.Plane planeFromRays(Vector3d a, Vector3d b) {
        // plane normal = b × a (use correct winding)
        Vector3d n = new Vector3d(b).cross(a).normalize();
        return new PixelFrustum.Plane(n, C);
    }
}
