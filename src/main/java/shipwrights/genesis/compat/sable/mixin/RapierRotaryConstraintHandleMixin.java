package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// Sable's physics engine can't be told that a hinge to the world has moved without changing how it turns. So a
/// sub-level about to be held to the world by one joins the world's frame first, and stays there for as long as it's
/// held (see {@link RapierConstraintCreationMixin})
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.rotary.RapierRotaryConstraintHandle", remap = false)
public abstract class RapierRotaryConstraintHandleMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private static void genesis$joinWorldFrame(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) ServerLevel level,
                                               @Local(argsOnly = true, ordinal = 0) PhysicsPipelineBody first,
                                               @Local(argsOnly = true, ordinal = 1) PhysicsPipelineBody second) {
        if ((first == null) != (second == null)) {
            PhysicsFrames.toWorldFrame(level, first == null ? second : first);
        }
    }
}
