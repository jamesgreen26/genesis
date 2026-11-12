package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.compat.sable.OutlinePrecision;

/// Sable moves what's about to be drawn onto a sub-level, and then by where the camera is, to be taken away again
/// by what draws it. An outline being drawn from somewhere nearer is moved by that instead: see
/// {@link OutlinePrecision}. Should Sable change so that this no longer applies, outlines are drawn as they were
@Mixin(targets = "dev.ryanhcode.sable.util.SublevelRenderOffsetHelper", remap = false)
public abstract class SublevelRenderOffsetHelperMixin {

    /// The second move: the first is to the sub-level, from the camera
    @WrapOperation(method = "posePlotToProjected", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 1), require = 0)
    private static void genesis$moveByWhereDrawnFrom(PoseStack pose, double x, double y, double z,
                                                     Operation<Void> original,
                                                     @Local(argsOnly = true) SubLevel subLevel) {
        Vec3 from = OutlinePrecision.movingOnto(subLevel);
        if (from == null) {
            original.call(pose, x, y, z);
        } else {
            original.call(pose, from.x, from.y, from.z);
        }
    }
}
