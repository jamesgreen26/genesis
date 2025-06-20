package g_mungus.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import g_mungus.genesis.GenesisMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Level level();

    @Unique
    private boolean genesis$isInSpace() {
        return level().dimension().location().equals(GenesisMod.SPACE_DIM);
    }

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void onBelowWorldMixin(CallbackInfo ci) {
        if(genesis$isInSpace()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "getGravity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isNoGravity()Z"))
    private boolean getGravityMixin(boolean original) {
        return original || genesis$isInSpace();
    }
}