package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Level level();

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void onBelowWorldMixin(CallbackInfo ci) {
        if(GenesisMod.shouldCancelVoidDamage(level())) {
            ci.cancel();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mixinEntityInit(EntityType arg, Level arg2, CallbackInfo ci) {
        GenesisMod.refreshEntityScaling(((Entity)(Object)this), level());
    }
}