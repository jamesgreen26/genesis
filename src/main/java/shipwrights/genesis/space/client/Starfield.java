package shipwrights.genesis.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import shipwrights.genesis.space.VantagePoint;

import java.util.ArrayList;
import java.util.List;

/// The distant stars, fixed in space: three layers of randomly placed points, each with its own tint, on a sphere
/// around the camera. Replaces vanilla's stars wherever celestials are drawn.
public final class Starfield {

    private static final List<Vector4fc> LAYER_COLORS = List.of(
            new Vector4f(1f, 1f, 1f, 0.8f),
            new Vector4f(0.8f, 0.8f, 1f, 0.8f),
            new Vector4f(1f, 1f, 0.8f, 0.8f)
    );

    private static final List<VertexBuffer> layers = new ArrayList<>();

    private Starfield() {
    }

    /// Draws the stars as seen from {@code vantagePoint}, adding their light to what's already drawn. Doesn't write
    /// depth, so draw them before anything they should be behind.
    ///
    /// @param brightness from 0 for invisible to 1 for fully bright
    public static void render(PoseStack poseStack, VantagePoint vantagePoint, float brightness) {
        if (brightness <= 0f) return;

        if (layers.isEmpty()) {
            for (int i = 0; i < LAYER_COLORS.size(); i++) {
                layers.add(buildLayer(10842L / (i + 4)));
            }
        }

        // Fixed in space, so seen from a celestial's surface they turn with it
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(vantagePoint.getRotation()).conjugate());

        float fogStart = RenderSystem.getShaderFogStart();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        ShaderInstance shader = GameRenderer.getPositionShader();
        // Applying a shader applies its blend mode, unless that mode was the last one applied, and the position shader
        // asks for none. Apply it now, so applying it again to draw changes nothing and leaves our blending be.
        shader.apply();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        for (int i = 0; i < layers.size(); i++) {
            Vector4fc color = LAYER_COLORS.get(i);
            float intensity = color.w() * brightness;
            RenderSystem.setShaderColor(color.x() * intensity, color.y() * intensity, color.z() * intensity, 1f);
            VertexBuffer layer = layers.get(i);
            layer.bind();
            layer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader);
        }
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderFogStart(fogStart);
        poseStack.popPose();
    }

    /// 1600 attempts at a star, randomly oriented squares on a sphere of radius 100, as in vanilla
    private static VertexBuffer buildLayer(long seed) {
        RandomSource random = RandomSource.create(seed);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int i = 0; i < 1600; ++i) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            double size = 0.05F + random.nextFloat() * 0.2F;
            double lengthSquared = x * x + y * y + z * z;
            if (lengthSquared < 1.0 && lengthSquared > 0.01) {
                double scale = 1.0 / Math.sqrt(lengthSquared);
                x *= scale;
                y *= scale;
                z *= scale;
                double centerX = x * 100.0;
                double centerY = y * 100.0;
                double centerZ = z * 100.0;
                double yaw = Math.atan2(x, z);
                double sinYaw = Math.sin(yaw);
                double cosYaw = Math.cos(yaw);
                double pitch = Math.atan2(Math.sqrt(x * x + z * z), y);
                double sinPitch = Math.sin(pitch);
                double cosPitch = Math.cos(pitch);
                double roll = random.nextDouble() * Math.PI * 2.0;
                double sinRoll = Math.sin(roll);
                double cosRoll = Math.cos(roll);

                for (int corner = 0; corner < 4; ++corner) {
                    double u = ((corner & 2) - 1) * size;
                    double v = ((corner + 1 & 2) - 1) * size;
                    double rolledU = u * cosRoll - v * sinRoll;
                    double rolledV = v * cosRoll + u * sinRoll;
                    double up = rolledU * sinPitch;
                    double out = -rolledU * cosPitch;
                    double offsetX = out * sinYaw - rolledV * cosYaw;
                    double offsetZ = rolledV * sinYaw + out * cosYaw;
                    builder.addVertex((float) (centerX + offsetX), (float) (centerY + up), (float) (centerZ + offsetZ));
                }
            }
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
