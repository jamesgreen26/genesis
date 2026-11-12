package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// Where a free joint holds a sub-level to the world, Sable's physics engine is told where that is in the sub-level's
/// frame, rather than in its level: see {@link PhysicsFrames#worldJoint}. An id of -1 is the world's, and the
/// quaternion turns the first side
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.free.RapierFreeConstraintHandle", remap = false)
public abstract class RapierFreeConstraintHandleMixin {

    @WrapOperation(method = "create", at = @At(value = "INVOKE",
            target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;addFreeConstraint(JIIDDDDDDDDDD)J"))
    private static long genesis$holdInFrame(long scene, int first, int second, double x1, double y1, double z1,
                                            double x2, double y2, double z2, double qx, double qy, double qz, double qw,
                                            Operation<Long> original, @Local(argsOnly = true) ServerLevel level) {
        if ((first == -1) == (second == -1) || !PhysicsFrames.movesWorldJoints(level)) {
            return original.call(scene, first, second, x1, y1, z1, x2, y2, z2, qx, qy, qz, qw);
        }
        if (first == -1) {
            Vector3dc offset = PhysicsFrames.offset(level, second);
            long joint = original.call(scene, first, second, x1 - offset.x(), y1 - offset.y(), z1 - offset.z(),
                    x2, y2, z2, qx, qy, qz, qw);
            PhysicsFrames.worldJoint(level, scene, joint, second, 0, x1, y1, z1, qx, qy, qz, qw);
            return joint;
        }
        Vector3dc offset = PhysicsFrames.offset(level, first);
        long joint = original.call(scene, first, second, x1, y1, z1,
                x2 - offset.x(), y2 - offset.y(), z2 - offset.z(), qx, qy, qz, qw);
        PhysicsFrames.worldJoint(level, scene, joint, first, 1, x2, y2, z2, 0, 0, 0, 1);
        return joint;
    }
}
