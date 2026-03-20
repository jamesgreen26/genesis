package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyVariable(
            method = "renderSnowAndRain",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            ),
            name = "f")
    private float modifyK2(float f, LightTexture lightTexture,
                           float partialTick,
                           double camX,
                           double camY,
                           double camZ) {
        double densityFade = 1.0 - Mth.clamp((camY - 300.0) / 60, 0.0, 1.0);

        return (float) (f * densityFade);
    }

    @WrapMethod(method = "renderClouds")
    public void genesis$renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick,
                                     double camX, double camY, double camZ, Operation<Void> original) {

        float fade = 1.0f - (float)((camY - 320.0) / 80.0);
        fade = Math.max(0.0f, Math.min(1.0f, fade));

        // Apply fade BEFORE rendering
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);

        original.call(poseStack, projectionMatrix, partialTick, camX, camY, camZ);

        // Reset after
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
