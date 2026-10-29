package shipwrights.genesis.space.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

/// Where a celestial is drawn, relative to the camera and in the camera's world orientation.
///
/// @param halfExtent half the side length of the cube it's drawn as
/// @param alpha how much of the celestial is drawn, as opposed to fog
public record CelestialPlacement(Vector3dc position, Quaterniondc rotation, double halfExtent, float alpha) {

    public static CelestialPlacement of(CelestialRenderContext context) {
        Celestial toRender = context.toRender();
        VantagePoint vantagePoint = context.vantagePoint();
        Camera camera = context.camera();

        Registry<Celestial> registry = context.clientLevel().registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Vector3dc position = toRender.transformProvider().getPosition(registry, context.time());
        Quaterniondc rotation = toRender.transformProvider().getRotation(registry, context.time());

        // Special case: the celestial underfoot is locked at a fixed position in world space, below the terrain
        if (vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial() == toRender) {
            double halfExtent = Minecraft.getInstance().gameRenderer.getRenderDistance();
            int buildHeight = context.clientLevel().getMaxBuildHeight();
            float alphaInterpolateStart = (float) (halfExtent + buildHeight / 3f);
            float alphaInterpolateEnd = (float) (halfExtent + buildHeight);

            float cameraY = (float) camera.getPosition().y;
            float alpha = Math.max(0f, Math.min(1f, (cameraY - alphaInterpolateStart) / (alphaInterpolateEnd - alphaInterpolateStart)));

            return new CelestialPlacement(
                    new Vector3d(0, - (camera.getPosition().y / 16) - halfExtent - 32, 0),
                    // The celestial as seen from the observer's frame
                    new Quaterniond(vantagePoint.getRotation()).conjugate().mul(rotation),
                    halfExtent,
                    alpha
            );
        }

        double halfExtent = toRender.size() / 2;

        // In space: celestials are at absolute positions, so subtract camera position
        if (vantagePoint instanceof VantagePoint.InSpace) {
            Vec3 cameraPos = camera.getPosition();
            return new CelestialPlacement(new Vector3d(position).sub(cameraPos.x, cameraPos.y, cameraPos.z), rotation, halfExtent, 1f);
        }

        // On a celestial: transform by the inverse of the vantage point
        Quaterniond inverseVantageRot = new Quaterniond(vantagePoint.getRotation()).conjugate();
        Vector3d relativePos = inverseVantageRot.transform(new Vector3d(position).sub(vantagePoint.getPosition()));
        return new CelestialPlacement(relativePos, new Quaterniond(inverseVantageRot).mul(rotation), halfExtent, 1f);
    }
}
