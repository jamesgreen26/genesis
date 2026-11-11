package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.CelestialRenderDispatcher;

/// Draws celestials behind the sky, then screen blends the sky over them ({@code sky + celestial * (1 - sky)}), like an
/// atmosphere scattering light. Under a dark sky celestials show as they are, and the brighter the sky the more it
/// hides them.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    private ClientLevel level;

    @Unique
    private static VertexBuffer genesis$belowSkyDisc;

    // Before the sky disc is bound, since drawing it uses whatever vertex buffer is bound
    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;bind()V", ordinal = 0))
    private void genesis$renderCelestialsBehindSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
                                                   boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        float[] skyColor = RenderSystem.getShaderColor().clone();

        // Nothing has been drawn yet this frame, so this only replaces the fog colour the screen was cleared to
        RenderSystem.clearColor(0, 0, 0, 1);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        CelestialRenderDispatcher.renderBehindSky(poseStack, camera, partialTick);

        // Restore what vanilla set up for the sky
        RenderSystem.setShaderColor(skyColor[0], skyColor[1], skyColor[2], skyColor[3]);
        RenderSystem.depthMask(false);
    }

    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", ordinal = 0))
    private void genesis$drawSkyAdditively(VertexBuffer skyDisc, Matrix4f pose, Matrix4f projection, ShaderInstance shader,
                                           Operation<Void> original) {
        // Applying a shader applies its blend mode, unless that mode was the last one applied, and the sky's shader asks
        // for alpha blending. Apply it now, so applying it again to draw changes nothing and leaves our blending be.
        shader.apply();
        // Celestials are drawn at the nearest depth (see CelestialRenderDispatcher.renderBehindSky); the sky covers them
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        original.call(skyDisc, pose, projection, shader);

        // The sky disc stops just above the horizon, and vanilla leaves the rest showing the clear colour, which is
        // now black. Blend the fog colour in there instead.
        float[] fogColor = RenderSystem.getShaderFogColor();
        RenderSystem.setShaderColor(fogColor[0], fogColor[1], fogColor[2], 1);
        RenderSystem.disableCull();
        VertexBuffer belowSkyDisc = genesis$getBelowSkyDisc();
        belowSkyDisc.bind();
        belowSkyDisc.drawWithShader(pose, projection, shader);
        RenderSystem.enableCull();

        // Vanilla draws the sunrise glow next, expecting the default alpha blending, which would hide what's behind it.
        // Make it additive, like vanilla's sun, moon and stars; screen blending would darken celestials where it fades.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    // Vanilla's sun and moon; celestials replace them. The first call draws the sunrise glow, which we keep.
    @WrapOperation(method = "renderSky", at = {
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V", ordinal = 1),
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V", ordinal = 2)
    })
    private void genesis$skipSunAndMoon(BufferBuilder.RenderedBuffer buffer, Operation<Void> original) {
        // Drawing would release the buffer; the builder can't be reused until it is
        buffer.release();
    }

    // Starfield replaces vanilla's stars wherever celestials are drawn. Skips drawing them rather than zeroing their
    // brightness, which other mods raise afterwards: ZPS brings the stars out as the air thins.
    @Definition(id = "starBuffer", field = "Lnet/minecraft/client/renderer/LevelRenderer;starBuffer:Lcom/mojang/blaze3d/vertex/VertexBuffer;")
    @Definition(id = "drawWithShader", method = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V")
    @Expression("this.starBuffer.drawWithShader(?, ?, ?)")
    @WrapOperation(method = "renderSky", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void genesis$hideVanillaStars(VertexBuffer starBuffer, Matrix4f pose, Matrix4f projection, ShaderInstance shader,
                                          Operation<Void> original) {
        if (GenesisMod.celestial(level).isEmpty()) {
            original.call(starBuffer, pose, projection, shader);
        }
    }

    // After the sunrise glow, before the stars: those are depth tested, so celestials hide them
    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V", ordinal = 0))
    private void genesis$depthTestSunMoonAndStars(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera,
                                                  boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        RenderSystem.enableDepthTest();
    }

    /// A cone from far below the camera up to the edge of vanilla's sky disc, sharing its edge vertices so the two
    /// meet without gaps or overlap.
    @Unique
    private static VertexBuffer genesis$getBelowSkyDisc() {
        if (genesis$belowSkyDisc == null) {
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            builder.vertex(0.0D, -512.0D, 0.0D).endVertex();

            // Matches LevelRenderer.buildSkyDisc(builder, 16)
            for (int i = -180; i <= 180; i += 45) {
                float angle = i * Mth.DEG_TO_RAD;
                builder.vertex(512.0F * Mth.cos(angle), 16.0D, 512.0F * Mth.sin(angle)).endVertex();
            }

            genesis$belowSkyDisc = new VertexBuffer(VertexBuffer.Usage.STATIC);
            genesis$belowSkyDisc.bind();
            genesis$belowSkyDisc.upload(builder.end());
            VertexBuffer.unbind();
        }
        return genesis$belowSkyDisc;
    }
}
