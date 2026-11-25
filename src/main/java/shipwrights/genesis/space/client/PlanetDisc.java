package shipwrights.genesis.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import shipwrights.genesis.space.VantagePoint;

/// The celestial the observer is standing on, seen from above: a disc of fog colour straight below the camera, hiding
/// the stars and celestials behind it. The level isn't drawn far enough to cover them itself.
///
/// It's the same size whatever the celestial, shrinking a little as the camera climbs. Its edge fades out, rather than
/// cutting across the sky.
public final class PlanetDisc {

    /// The angle, in radians, from straight down to the middle of the disc's edge, with the camera at the horizon height
    private static final float GROUND_ANGULAR_RADIUS = 60f * Mth.DEG_TO_RAD;
    /// What that angle shrinks towards as the camera climbs ever higher
    private static final float MIN_ANGULAR_RADIUS = 20f * Mth.DEG_TO_RAD;
    /// How far, in blocks above the horizon height, the camera climbs for the disc to shrink halfway to its smallest
    private static final double HALF_SHRINK_HEIGHT = 512.0;
    /// How far, in radians, the edge fades over, centred on the middle of the edge
    private static final float EDGE_SOFTNESS = 10f * Mth.DEG_TO_RAD;
    private static final int SEGMENTS = 64;
    private static final int EDGE_RINGS = 8;
    /// Any distance inside the near and far planes will do; it's drawn without depth testing
    private static final float RADIUS = 10f;

    private PlanetDisc() {}

    /// @param poseStack holding the camera's rotation only, as the sky is drawn with
    public static void render(PoseStack poseStack, ClientLevel level, VantagePoint vantagePoint, double cameraY) {
        if (!(vantagePoint instanceof VantagePoint.OnCelestial)) return;

        double height = Math.max(cameraY - level.getLevelData().getHorizonHeight(level), 0.0);
        float groundness = (float) (HALF_SHRINK_HEIGHT / (HALF_SHRINK_HEIGHT + height));
        float angularRadius = Mth.lerp(groundness, MIN_ANGULAR_RADIUS, GROUND_ANGULAR_RADIUS);

        float inner = angularRadius - EDGE_SOFTNESS / 2f;
        float outer = angularRadius + EDGE_SOFTNESS / 2f;

        float[] fogColor = RenderSystem.getShaderFogColor();
        float red = fogColor[0], green = fogColor[1], blue = fogColor[2];

        Matrix4f pose = poseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();

        // Solid up to where the edge starts fading
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(pose, 0, -RADIUS, 0).color(red, green, blue, 1f).endVertex();
        for (int i = 0; i <= SEGMENTS; i++) {
            addRingVertex(builder, pose, inner, i, red, green, blue, 1f);
        }
        BufferUploader.drawWithShader(builder.end());

        // The edge, fading out smoothly
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int ring = 0; ring < EDGE_RINGS; ring++) {
            float t0 = ring / (float) EDGE_RINGS;
            float t1 = (ring + 1) / (float) EDGE_RINGS;
            float angle0 = Mth.lerp(t0, inner, outer);
            float angle1 = Mth.lerp(t1, inner, outer);
            float alpha0 = 1f - smoothstep(t0);
            float alpha1 = 1f - smoothstep(t1);
            for (int i = 0; i <= SEGMENTS; i++) {
                addRingVertex(builder, pose, angle0, i, red, green, blue, alpha0);
                addRingVertex(builder, pose, angle1, i, red, green, blue, alpha1);
            }
            // Degenerate triangles join this ring to the next
            addRingVertex(builder, pose, angle1, SEGMENTS, red, green, blue, alpha1);
            addRingVertex(builder, pose, angle1, 0, red, green, blue, alpha1);
        }
        BufferUploader.drawWithShader(builder.end());

        // The level drawn next expects depth writes on, as the dispatcher leaves them
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /// @param fromNadir the angle from straight down
    private static void addRingVertex(BufferBuilder builder, Matrix4f pose, float fromNadir, int segment,
                                      float red, float green, float blue, float alpha) {
        float around = segment * Mth.TWO_PI / SEGMENTS;
        float horizontal = RADIUS * Mth.sin(fromNadir);
        builder.vertex(pose, horizontal * Mth.cos(around), -RADIUS * Mth.cos(fromNadir), horizontal * Mth.sin(around))
                .color(red, green, blue, alpha).endVertex();
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }
}
