package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getDepthFar", at = @At("HEAD"), cancellable = true)
    public void getDepthFarMixin(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Float.POSITIVE_INFINITY);
    }
}
