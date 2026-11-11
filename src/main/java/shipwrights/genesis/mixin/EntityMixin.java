package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.ThinIceBlock;
import shipwrights.genesis.space.travel.CelestialTravel;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract Level level();

    /// Space has no void: nothing that drifts below the bottom of the world is hurt (living entities) or removed
    /// (everything else). Every entity's check for the void comes through here
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void genesis$noVoidInSpace(CallbackInfo ci) {
        if (GenesisMod.isSpaceLikeDimension(level())) {
            ci.cancel();
        }
    }

    /// Vanilla won't let anything riding or ridden change dimension, but travel to and from space takes everything
    /// riding along. While it checks what's riding, leave that out, keeping whatever else subclasses rule out, as they
    /// add to this
    @Inject(method = "canChangeDimensions", at = @At("HEAD"), cancellable = true)
    private void genesis$travelWithRiders(CallbackInfoReturnable<Boolean> cir) {
        if (CelestialTravel.isCheckingStack()) {
            cir.setReturnValue(true);
        }
    }

    // The thin ice handling below runs for players on the server too, from their movement packets

    /// Vanilla only adds to the fall distance while still in the air, leaving out the last move, onto the ground. So
    /// something that has just broken through thin ice and hits more below it within a tick or two, as down a tower of
    /// it, would seem to have barely fallen, if at all, and land on it. Count that last move onto thin ice, so that it
    /// breaks through each in turn, and so that none of the fall goes uncounted
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void genesis$countLandingOnThinIce(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (onGround && y < 0.0D && state.getBlock() instanceof ThinIceBlock) {
            ((Entity) (Object) this).fallDistance -= (float) y;
        }
    }

    /// Landing resets the fall distance, but breaking through thin ice doesn't end the fall, so it carries on counting,
    /// and whatever broke through takes the damage of the whole fall when it lands. Unless something else still holds
    /// it up, in which case the fall ends here, as {@link ThinIceBlock#fallOn} deals its damage
    @WrapWithCondition(method = "checkFallDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;resetFallDistance()V"))
    private boolean genesis$keepFallingThroughThinIce(Entity entity, @Local(argsOnly = true) BlockState state,
                                                      @Local(argsOnly = true) BlockPos pos) {
        return !(state.getBlock() instanceof ThinIceBlock) || !ThinIceBlock.brokeThrough(entity, pos);
    }
}
