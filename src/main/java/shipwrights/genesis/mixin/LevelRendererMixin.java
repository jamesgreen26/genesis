package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyVariable(
            method = "renderSnowAndRain",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            ),
            name = "f")
    private float modifyK2(float f, LightTexture lightTexture,
                           float partialTick,
                           double camX,
                           double camY,
                           double camZ) {
        double densityFade = 1.0 - Mth.clamp((camY - 300.0) / 60, 0.0, 1.0);

        return (float) (f * densityFade);
    }
}
