package shipwrights.genesis.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/// Sable's physics engine itself, for what Sable gives no other way to tell it
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.Rapier3D", remap = false)
public interface Rapier3DInvoker {
    /// Tells the engine where the joint {@code joint} of {@code scene} holds what it does on {@code side}, the first
    /// being 0, and how it's turned there: measured from a sub-level, or if it's the world that's held to, from the
    /// engine's origin
    @Invoker("setConstraintFrame")
    static void genesis$setConstraintFrame(long scene, long joint, int side, double x, double y, double z,
                                           double qx, double qy, double qz, double qw) {
        throw new AssertionError();
    }
}
