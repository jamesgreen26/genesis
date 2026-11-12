package shipwrights.genesis.compat.simulated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// The physics staff drags a sub-level by a joint to the world at the level's origin, driven to where in the level
/// the sub-level should be. Far out, that's a number too coarse to move anything smoothly by: ten million blocks
/// out, it only comes in whole blocks. So in space, where Sable's physics engine has each sub-level in a frame near
/// its own origin (see {@link PhysicsFrames}), the joint is to the origin of the sub-level's frame instead, and
/// driven to where in the frame the sub-level should be: the same place, in numbers as fine as the engine's
@Mixin(targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler$DragSession", remap = false)
public abstract class PhysicsStaffDragSessionMixin {
    @Shadow
    @Final
    private ServerSubLevel subLevel;

    /// The joint is made at the engine's origin, as it's told to be, rather than moved to the level's
    @WrapOperation(method = "attachConstraint", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/physics/PhysicsPipeline;addConstraint(Ldev/ryanhcode/sable/api/physics/PhysicsPipelineBody;Ldev/ryanhcode/sable/api/physics/PhysicsPipelineBody;Ldev/ryanhcode/sable/api/physics/constraint/PhysicsConstraintConfiguration;)Ldev/ryanhcode/sable/api/physics/constraint/PhysicsConstraintHandle;"))
    private PhysicsConstraintHandle genesis$jointToFrame(PhysicsPipeline pipeline, PhysicsPipelineBody first,
                                                         PhysicsPipelineBody second,
                                                         PhysicsConstraintConfiguration<?> configuration,
                                                         Operation<PhysicsConstraintHandle> original) {
        boolean was = PhysicsFrames.localJoints(true);
        try {
            return original.call(pipeline, first, second, configuration);
        } finally {
            PhysicsFrames.localJoints(was);
        }
    }

    /// Where the sub-level should be is worked out in the level, from where the player is: this is the last step of
    /// that, before it's turned to the joint's axes and the joint driven to it
    @WrapOperation(method = "physicsTick", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;add(DDD)Lorg/joml/Vector3d;"))
    private Vector3d genesis$goalInFrame(Vector3d goal, double x, double y, double z, Operation<Vector3d> original) {
        return original.call(goal, x, y, z).sub(PhysicsFrames.offset(subLevel.getLevel(), subLevel.getRuntimeId()));
    }
}
