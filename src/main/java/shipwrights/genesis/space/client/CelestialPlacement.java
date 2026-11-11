package shipwrights.genesis.space.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

/// Where a celestial is drawn, relative to the camera and in the camera's world orientation.
///
/// The celestial the observer is standing on isn't drawn, so has no placement.
///
/// @param halfExtent half the side length of the cube it's drawn as
public record CelestialPlacement(Vector3dc position, Quaterniondc rotation, double halfExtent) {

    /// How much celestials in the sky show through the weather, seen from {@code cameraY}: rain and thunder hide them
    /// near the ground, less so the higher up the camera is, until it's high enough above the terrain to be above the
    /// weather. Always 1 away from a celestial's surface.
    public static float weatherVisibility(ClientLevel level, VantagePoint vantagePoint, double cameraY, float partialTick) {
        if (!(vantagePoint instanceof VantagePoint.OnCelestial)) return 1f;
        double renderDistance = Minecraft.getInstance().gameRenderer.getRenderDistance();
        int buildHeight = level.getMaxBuildHeight();
        double start = renderDistance + buildHeight / 3.0;
        double end = renderDistance + buildHeight;
        float aboveWeather = (float) Mth.clamp((cameraY - start) / (end - start), 0.0, 1.0);
        // Thunder only happens while it's raining, so the rain level covers both
        float rain = level.getRainLevel(partialTick);
        return 1f - rain * (1f - aboveWeather);
    }

    /// How bright the sky is, from 0 when it's dark, as at night, to 1 in full daylight. The brightest channel of the
    /// sky colour, which vanilla scales by daylight and darkens with rain and thunder.
    public static float skyBrightness(ClientLevel level, Vec3 cameraPosition, float partialTick) {
        Vec3 sky = level.getSkyColor(cameraPosition, partialTick);
        return (float) Mth.clamp(Math.max(sky.x, Math.max(sky.y, sky.z)), 0.0, 1.0);
    }

    public static CelestialPlacement of(CelestialRenderContext context) {
        Celestial toRender = context.toRender();
        VantagePoint vantagePoint = context.vantagePoint();
        Camera camera = context.camera();

        Registry<Celestial> registry = context.clientLevel().registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Vector3dc position = toRender.transformProvider().getPosition(registry, context.time());
        Quaterniondc rotation = toRender.transformProvider().getRotation(registry, context.time());

        double halfExtent = toRender.size() / 2;

        // In space: celestials are at absolute positions, so subtract camera position
        if (vantagePoint instanceof VantagePoint.InSpace) {
            Vec3 cameraPos = camera.getPosition();
            return new CelestialPlacement(new Vector3d(position).sub(cameraPos.x, cameraPos.y, cameraPos.z), rotation, halfExtent);
        }

        // On a celestial: transform by the inverse of the vantage point
        Quaterniond inverseVantageRot = new Quaterniond(vantagePoint.getRotation()).conjugate();
        Vector3d relativePos = inverseVantageRot.transform(new Vector3d(position).sub(vantagePoint.getPosition()));
        return new CelestialPlacement(relativePos, new Quaterniond(inverseVantageRot).mul(rotation), halfExtent);
    }
}
