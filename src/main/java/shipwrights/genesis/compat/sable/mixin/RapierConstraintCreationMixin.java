package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// A joint is made in Sable's physics engine between two sub-levels, measured from each, or between one and the
/// world, measured from where it is in its level. So sub-levels being joined share a frame first, rather than by the
/// next tick, and one being held to the world joins the world's frame, and stays there for as long as it's held (see
/// {@link PhysicsFrames})
@Mixin(targets = {
        "dev.ryanhcode.sable.physics.impl.rapier.constraint.fixed.RapierFixedConstraintHandle",
        "dev.ryanhcode.sable.physics.impl.rapier.constraint.free.RapierFreeConstraintHandle",
        "dev.ryanhcode.sable.physics.impl.rapier.constraint.rotary.RapierRotaryConstraintHandle",
        "dev.ryanhcode.sable.physics.impl.rapier.constraint.generic.RapierGenericConstraintHandle"
}, remap = false)
public abstract class RapierConstraintCreationMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private static void genesis$shareFrame(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) ServerLevel level,
                                           @Local(argsOnly = true, ordinal = 0) PhysicsPipelineBody first,
                                           @Local(argsOnly = true, ordinal = 1) PhysicsPipelineBody second) {
        PhysicsFrames.joining(level, first, second);
    }

    @Inject(method = "create", at = @At("RETURN"))
    private static void genesis$holdToWorldFrame(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) ServerLevel level,
                                                 @Local(argsOnly = true, ordinal = 0) PhysicsPipelineBody first,
                                                 @Local(argsOnly = true, ordinal = 1) PhysicsPipelineBody second) {
        Object joint = cir.getReturnValue();
        if (joint != null && (first == null || second == null)) {
            PhysicsFrames.pin(level, first == null ? second : first, joint, "");
        }
    }
}
