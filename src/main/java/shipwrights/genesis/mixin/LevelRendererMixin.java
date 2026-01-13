package shipwrights.genesis.mixin;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import shipwrights.genesis.client.PlanetRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    // taken from veil
    @Inject(method = "prepareCullFrustum", at = @At("HEAD"), cancellable = true)
    public void captureViewMatrix(PoseStack modelViewStack, Vec3 pos, Matrix4f projection, CallbackInfo ci) {
        Matrix4f viewMatrix = modelViewStack.last().pose();
        Matrix3f viewMatrix3f = new Matrix3f(viewMatrix);
        PlanetRenderer.inverseViewMatrix = viewMatrix3f.invert();
    }
}