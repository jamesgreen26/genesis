package shipwrights.genesis.compat.catnip.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.compat.sable.OutlinePrecision;

/// Catnip's outliner draws each outline it's showing, in the slot it's shown in, from where the camera is. One on a
/// Sable sub-level is drawn from somewhere nearer instead: see {@link OutlinePrecision}. Should Catnip change so that
/// this no longer applies, outlines are drawn as they were
@Mixin(targets = "net.createmod.catnip.outliner.Outliner", remap = false)
public abstract class OutlinerMixin {
    private static final String RENDER = "Lnet/createmod/catnip/outliner/Outline;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;F)V";

    /// Each outline is drawn by what's done for each slot in {@code renderOutlines}
    private static final String EACH = "lambda$renderOutlines$0";

    @ModifyArg(method = EACH, at = @At(value = "INVOKE", target = RENDER), index = 2, require = 0)
    private static Vec3 genesis$drawFromNear(Vec3 camera, @Local(argsOnly = true) Object slot) {
        return OutlinePrecision.begin(slot, camera);
    }

    @Inject(method = EACH, at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER), require = 0)
    private static void genesis$drawn(CallbackInfo ci) {
        OutlinePrecision.end();
    }
}
