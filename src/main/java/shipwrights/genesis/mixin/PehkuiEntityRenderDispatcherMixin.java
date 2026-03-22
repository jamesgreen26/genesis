package shipwrights.genesis.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;

@Mixin(value = EntityRenderDispatcher.class, priority = 1500)
public class PehkuiEntityRenderDispatcherMixin {

    @Unique
    private static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();

    @TargetHandler(
            mixin = "virtuoel.pehkui.mixin.client.compat115plus.EntityRenderDispatcherMixin",
            name = "pehkui$render$before"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"))
    private <E extends Entity> void beforeRender(E entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo info, CallbackInfo ci) {
        CURRENT_ENTITY.set(entity);
    }

    @TargetHandler(
            mixin = "virtuoel.pehkui.mixin.client.compat115plus.EntityRenderDispatcherMixin",
            name = "pehkui$render$before"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            )
    )
    private void cancelScaling(PoseStack instance, float xScale, float yScale, float zScale,
                               Operation<Void> original) {
        Entity entity = CURRENT_ENTITY.get();
        if (GenesisMod.isMiniScale(entity.level()) && ((entity instanceof LivingEntity && entity.isPassenger())
                || entity.getType().builtInRegistryHolder().key().location().equals(ResourceLocation.parse("create:stationary_contraption")))) {
            return;
        }
        original.call(instance, xScale, yScale, zScale);
    }

    @TargetHandler(
            mixin = "virtuoel.pehkui.mixin.client.compat115plus.EntityRenderDispatcherMixin",
            name = "pehkui$render$before"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"))
    private void afterRender(CallbackInfo ci) {
        CURRENT_ENTITY.remove();
    }
}

