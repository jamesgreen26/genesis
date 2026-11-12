package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// Where a generic joint holds a sub-level to the world, Sable's physics engine is told where that is in the
/// sub-level's frame, rather than in its level, as it's made and whenever it's moved: see
/// {@link PhysicsFrames#worldJoint}. An id of -1 is the world's
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.generic.RapierGenericConstraintHandle", remap = false)
public abstract class RapierGenericConstraintHandleMixin {
    private static final String RAPIER = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;";

    @WrapOperation(method = "create", at = @At(value = "INVOKE", target = RAPIER + "addGenericConstraint(JIIDDDDDDDDDDDDDDI)J"))
    private static long genesis$holdInFrame(long scene, int first, int second,
                                            double x1, double y1, double z1, double qx1, double qy1, double qz1, double qw1,
                                            double x2, double y2, double z2, double qx2, double qy2, double qz2, double qw2,
                                            int locked, Operation<Long> original,
                                            @Local(argsOnly = true) ServerLevel level) {
        if ((first == -1) == (second == -1) || !PhysicsFrames.movesWorldJoints(level)) {
            return original.call(scene, first, second, x1, y1, z1, qx1, qy1, qz1, qw1, x2, y2, z2, qx2, qy2, qz2, qw2, locked);
        }
        if (first == -1) {
            Vector3dc offset = PhysicsFrames.offset(level, second);
            long joint = original.call(scene, first, second, x1 - offset.x(), y1 - offset.y(), z1 - offset.z(),
                    qx1, qy1, qz1, qw1, x2, y2, z2, qx2, qy2, qz2, qw2, locked);
            PhysicsFrames.worldJoint(level, scene, joint, second, 0, x1, y1, z1, qx1, qy1, qz1, qw1);
            return joint;
        }
        Vector3dc offset = PhysicsFrames.offset(level, first);
        long joint = original.call(scene, first, second, x1, y1, z1, qx1, qy1, qz1, qw1,
                x2 - offset.x(), y2 - offset.y(), z2 - offset.z(), qx2, qy2, qz2, qw2, locked);
        PhysicsFrames.worldJoint(level, scene, joint, first, 1, x2, y2, z2, qx2, qy2, qz2, qw2);
        return joint;
    }

    @WrapOperation(method = {"setFrame1", "setFrame2"}, at = @At(value = "INVOKE", target = RAPIER + "setConstraintFrame(JJIDDDDDDD)V"))
    private void genesis$moveInFrame(long scene, long joint, int side, double x, double y, double z,
                                     double qx, double qy, double qz, double qw, Operation<Void> original) {
        Vector3dc offset = PhysicsFrames.worldJointMoved(this, side, x, y, z, qx, qy, qz, qw);
        original.call(scene, joint, side, x - offset.x(), y - offset.y(), z - offset.z(), qx, qy, qz, qw);
    }
}
