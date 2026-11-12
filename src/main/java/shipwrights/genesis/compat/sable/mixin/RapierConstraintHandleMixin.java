package shipwrights.genesis.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// A sub-level held to the world by a joint is let go of the world's frame as the joint is removed (see
/// {@link RapierConstraintCreationMixin})
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle", remap = false)
public abstract class RapierConstraintHandleMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void genesis$letGo(CallbackInfo ci) {
        PhysicsFrames.unpin(this);
    }
}
