package shipwrights.genesis.compat.sable.mixin;

import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.compat.sable.PhysicsFrames;

import java.util.List;

/// A rope is made in Sable's physics engine where it is in its level, and stays in the world's frame: so what's near
/// enough to touch it joins it there first, and what it's tied to stays there for as long as it is (see
/// {@link PhysicsFrames})
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.rope.RapierRopeHandle", remap = false)
public abstract class RapierRopeHandleMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private static void genesis$joinWorldFrame(long scene, double radius, List<Vector3d> points,
                                               CallbackInfoReturnable<?> cir) {
        if (points.isEmpty()) return;
        AABBd box = new AABBd();
        for (Vector3d point : points) box.union(point);
        PhysicsFrames.worldObject(scene, box);
    }

    @Inject(method = "setAttachment", at = @At("HEAD"))
    private void genesis$holdToWorldFrame(RopeHandle.AttachmentPoint point, Vector3dc position, ServerSubLevel subLevel,
                                          CallbackInfo ci) {
        if (subLevel == null) {
            PhysicsFrames.unpin(this, point);
        } else {
            PhysicsFrames.pin(subLevel.getLevel(), subLevel, this, point);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void genesis$letGo(CallbackInfo ci) {
        PhysicsFrames.unpin(this);
    }
}
