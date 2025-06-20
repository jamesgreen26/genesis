package g_mungus.genesis.mixin;

import g_mungus.genesis.GenesisMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getDepthFar", at = @At("HEAD"), cancellable = true)
    public void getDepthFarMixin(CallbackInfoReturnable<Float> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
            cir.setReturnValue(50000f);
        }
    }
}
