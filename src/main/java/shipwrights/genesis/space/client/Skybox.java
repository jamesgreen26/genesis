package shipwrights.genesis.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.VantagePoint;

import java.util.List;

/// Faint, distant galaxies and nebulae behind the {@link Starfield}, fixed in space like it.
///
/// A cube around the camera, one texture per face at {@code genesis:textures/environment/skybox/<face>.png}. The
/// textures are low resolution and filtered linearly (see their {@code .png.mcmeta}), so they come out soft and blurry.
public final class Skybox {

    /// How strongly the skybox shows, at full brightness
    private static final float OPACITY = 0.35f;

    private static final float HALF_SIZE = 100f;

    /// Each face's texture is laid out by the direction at its top-left corner and the directions its u and v run
    /// along, as seen from inside the cube. Textures must be made with the same layout.
    private record Face(ResourceLocation texture, Vector3fc corner, Vector3fc u, Vector3fc v) {
        Face(String name, Vector3fc corner, Vector3fc u, Vector3fc v) {
            this(GenesisMod.id("textures/environment/skybox/" + name + ".png"), corner, u, v);
        }
    }

    private static final List<Face> FACES = List.of(
            new Face("north", new Vector3f(-1, 1, -1), new Vector3f(1, 0, 0), new Vector3f(0, -1, 0)),
            new Face("south", new Vector3f(1, 1, 1), new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0)),
            new Face("west", new Vector3f(-1, 1, 1), new Vector3f(0, 0, -1), new Vector3f(0, -1, 0)),
            new Face("east", new Vector3f(1, 1, -1), new Vector3f(0, 0, 1), new Vector3f(0, -1, 0)),
            new Face("up", new Vector3f(-1, 1, -1), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1)),
            new Face("down", new Vector3f(-1, -1, 1), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1))
    );

    private Skybox() {
    }

    /// Draws the skybox as seen from {@code vantagePoint}, adding its light to what's already drawn. Doesn't write
    /// depth, so draw it before anything it should be behind.
    ///
    /// @param brightness from 0 for invisible to 1 for fully bright
    public static void render(PoseStack poseStack, VantagePoint vantagePoint, float brightness) {
        float intensity = OPACITY * brightness;
        if (intensity <= 0f) return;

        // Fixed in space, so seen from a celestial's surface it turns with it
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(vantagePoint.getRotation()).conjugate());
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ShaderInstance shader = RenderSystem.getShader();
        // Applying a shader applies its blend mode, unless that mode was the last one applied, and the position texture
        // shader asks for none. Apply it now, so applying it again to draw changes nothing and leaves our blending be.
        shader.apply();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(intensity, intensity, intensity, 1f);

        for (Face face : FACES) {
            RenderSystem.setShaderTexture(0, face.texture());
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertex(builder, pose, face, 0, 0);
            vertex(builder, pose, face, 0, 1);
            vertex(builder, pose, face, 1, 1);
            vertex(builder, pose, face, 1, 0);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    private static void vertex(BufferBuilder builder, Matrix4f pose, Face face, float u, float v) {
        Vector3f position = new Vector3f(face.corner())
                .fma(2 * u, face.u())
                .fma(2 * v, face.v())
                .mul(HALF_SIZE);
        builder.addVertex(pose, position.x, position.y, position.z).setUv(u, v);
    }
}
