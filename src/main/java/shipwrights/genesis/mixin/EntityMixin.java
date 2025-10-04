package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Level level();

    @Unique
    private boolean genesis$isInSpace() {
        return level().dimension().location().equals(GenesisMod.SPACE_DIM);
    }

    @Unique
    private boolean genesis$isWithoutVoid() {
        return level().dimension().location().equals(GenesisMod.SPACE_DIM) ||
               level().dimension().location().equals(GenesisMod.WORMHOLE_DIM);
    }

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void onBelowWorldMixin(CallbackInfo ci) {
        if(genesis$isWithoutVoid()) {
            ci.cancel();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mixinEntityInit(EntityType arg, Level arg2, CallbackInfo ci) {
        GenesisMod.refreshEntityScaling(((Entity)(Object)this), genesis$isInSpace());
    }
}