package g_mungus.genesis.mixin;

import g_mungus.genesis.GenesisMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow public abstract Level level();

    @Unique
    private boolean genesis$isInSpace() {
        return level.dimension().location().equals(GenesisMod.SPACE_DIM);
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

    @ModifyVariable(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            index = 2, // 0 = 'this', 1 = MoverType, 2 = Vec3
            argsOnly = true
    )
    private Vec3 modifyMoveVector(Vec3 vec3, MoverType type) {
        if (genesis$isInSpace() && (type == MoverType.SELF || type == MoverType.PLAYER)) {
            return vec3.scale(1.0 / 16.0);
        }
        return vec3;
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"))
    private void startRidingMixin(Entity arg, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        GenesisMod.refreshEntityScaling((Entity)(Object) this, false);
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void removeVehicleMixin(CallbackInfo ci) {
        GenesisMod.refreshEntityScaling((Entity)(Object) this, genesis$isInSpace());
    }
}