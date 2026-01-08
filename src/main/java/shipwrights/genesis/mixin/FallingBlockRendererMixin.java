package shipwrights.genesis.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.mixin_extension.FallingBlockEntityExtension;

@Mixin(FallingBlockRenderer.class)
public class FallingBlockRendererMixin {

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
            ),
            method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    )
    private void applyRotation(FallingBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Vector3d rotation = ((FallingBlockEntityExtension) entity).genesis$getRotation();


        if (rotation.x != 0 || rotation.y != 0 || rotation.z != 0) {
            poseStack.translate(0.5, 0.5, 0.5);

            Quaternionf quat = new Quaternionf()
                    .rotateX((float) rotation.x)
                    .rotateY((float) rotation.y)
                    .rotateZ((float) rotation.z);

            poseStack.mulPose(quat);

            poseStack.translate(-0.5, -0.5, -0.5);
        }
    }
}
