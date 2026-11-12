package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// Has Sable's physics engine told where each sub-level is in its frame, rather than in its level: see
/// {@link PhysicsFrames}. Every position of a sub-level that goes to the engine, or comes back from it, passes
/// through one of these calls. The rest of what it's told of a sub-level, its blocks, mass, forces, and joints to
/// other sub-levels, is measured from the sub-level itself, and velocities are the same wherever they're measured
/// from
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class RapierPhysicsPipelineMixin {
    private static final String RAPIER = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;";

    @Shadow
    @Final
    private ServerLevel level;

    /// {@code pose} is the position, then the orientation
    @WrapOperation(method = "add(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/companion/math/Pose3dc;)V",
            at = @At(value = "INVOKE", target = RAPIER + "createSubLevel(JI[D)V"))
    private void genesis$createInFrame(long scene, int id, double[] pose, Operation<Void> original) {
        Vector3dc offset = PhysicsFrames.created(level, scene, id, pose[0], pose[1], pose[2]);
        pose[0] -= offset.x();
        pose[1] -= offset.y();
        pose[2] -= offset.z();
        original.call(scene, id, pose);
    }

    @WrapOperation(method = "remove(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;)V",
            at = @At(value = "INVOKE", target = RAPIER + "removeSubLevel(JI)V"))
    private void genesis$removeFromFrame(long scene, int id, Operation<Void> original) {
        original.call(scene, id);
        PhysicsFrames.removed(level, id);
    }

    @WrapOperation(method = "readPose", at = @At(value = "INVOKE", target = RAPIER + "getPose(JI[D)V"))
    private void genesis$readFromFrame(long scene, int id, double[] pose, Operation<Void> original) {
        original.call(scene, id, pose);
        Vector3dc offset = PhysicsFrames.offset(level, id);
        pose[0] += offset.x();
        pose[1] += offset.y();
        pose[2] += offset.z();
    }

    @WrapOperation(method = "teleport", at = @At(value = "INVOKE", target = RAPIER + "teleportObject(JIDDDDDDD)V"))
    private void genesis$teleportInFrame(long scene, int id, double x, double y, double z, double qx, double qy,
                                         double qz, double qw, Operation<Void> original) {
        Vector3dc offset = PhysicsFrames.teleported(level, id, x, y, z);
        original.call(scene, id, x - offset.x(), y - offset.y(), z - offset.z(), qx, qy, qz, qw);
    }
}
