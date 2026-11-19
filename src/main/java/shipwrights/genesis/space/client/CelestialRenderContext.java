package shipwrights.genesis.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.Time;

/// Everything a {@link CelestialVisualizer.Component} needs to draw a celestial.
///
/// @param celestials     every celestial, which positions are worked out from
/// @param cameraPosition where the camera is, in the level when on a celestial, or in space otherwise
/// @param clientLevel    the level being viewed from, or null when not viewed from a level at all, as on the
///                       {@linkplain shipwrights.genesis.space.client.map.SolarSystemScreen solar system map}. Only
///                       consulted when the vantage point is {@link VantagePoint.OnCelestial}, which always has one.
public record CelestialRenderContext(
        Celestial toRender,
        Registry<Celestial> celestials,
        VantagePoint vantagePoint,
        Time time,
        PoseStack poseStack,
        Vec3 cameraPosition,
        @Nullable ClientLevel clientLevel
) {
    /// How much celestials in the sky show through the weather; see {@link CelestialPlacement#weatherVisibility}.
    /// Always 1 without a level, which has no weather.
    public float weatherVisibility() {
        if (clientLevel == null) return 1f;
        return CelestialPlacement.weatherVisibility(clientLevel, vantagePoint, cameraPosition.y, time.partialTick());
    }

    /// How bright the sky is, from 0 when it's dark to 1 in full daylight; see {@link CelestialPlacement#skyBrightness}.
    /// Always 0 away from a celestial's surface, or without a level, where there's no sky.
    public float skyBrightness() {
        if (clientLevel == null || !(vantagePoint instanceof VantagePoint.OnCelestial)) return 0f;
        return CelestialPlacement.skyBrightness(clientLevel, cameraPosition, time.partialTick());
    }
}
