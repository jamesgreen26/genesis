package g_mungus.genesis.mixin;

import com.google.gson.JsonObject;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public class BlockElementDeserializerMixin {

    @Shadow
    private Vector3f getVector3f(JsonObject jsonObject, String from) {
        throw new RuntimeException("this method implementation should be overwritten at runtime.");
    }

    @Inject(method = "getFrom", at = @At("HEAD"), cancellable = true)
    private void injectGetFrom(JsonObject jsonObject, CallbackInfoReturnable<Vector3f> cir) {
        Vector3f vector = getVector3f(jsonObject, "from");
        cir.setReturnValue(vector);
    }

    @Inject(method = "getTo", at = @At("HEAD"), cancellable = true)
    private void injectGetTo(JsonObject jsonObject, CallbackInfoReturnable<Vector3f> cir) {
        Vector3f vector = getVector3f(jsonObject, "to");
        cir.setReturnValue(vector);
    }

}
