package shipwrights.genesis.compat.sable.mixin;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.compat.sable.PhysicsFrames;

/// A box is made in Sable's physics engine where it is in its level, and stays in the world's frame: so what's near
/// enough to touch it joins it there first (see {@link PhysicsFrames})
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.box.RapierBoxHandle", remap = false)
public abstract class RapierBoxHandleMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private static void genesis$joinWorldFrame(long scene, Pose3dc pose, Vector3dc halfExtents, double mass,
                                               CallbackInfoReturnable<?> cir) {
        // However it's turned
        double reach = halfExtents.length();
        Vector3dc at = pose.position();
        PhysicsFrames.worldObject(scene, new AABBd(at.x() - reach, at.y() - reach, at.z() - reach,
                at.x() + reach, at.y() + reach, at.z() + reach));
    }
}
