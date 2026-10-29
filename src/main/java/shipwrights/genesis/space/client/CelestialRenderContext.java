package shipwrights.genesis.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.Time;

public record CelestialRenderContext(
        Celestial toRender,
        VantagePoint vantagePoint,
        Time time,
        PoseStack poseStack,
        Camera camera,
        ClientLevel clientLevel
) {
}
