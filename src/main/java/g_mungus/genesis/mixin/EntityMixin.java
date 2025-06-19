package g_mungus.genesis.mixin;

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

    @Inject(method = "isNoGravity", at = @At("HEAD"), cancellable = true)
    public void isNoGravityMixin(CallbackInfoReturnable<Boolean> cir) {
        if (genesis$isInSpace()) {
            cir.setReturnValue(true);
        }
    }
}