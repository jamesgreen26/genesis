package shipwrights.genesis.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;

@Mixin(Sound.class)
public class SoundMixin {

    @Final
    @Shadow
    private int attenuationDistance;

    @Inject(method = "getAttenuationDistance", at = @At("HEAD"), cancellable = true)
    public void attenuateSounds(CallbackInfoReturnable<Integer> cir) {
        Level level = Minecraft.getInstance().level;
        if (level != null && GenesisMod.isMiniScale(level)) {
            cir.setReturnValue(attenuationDistance / 16);
        }
    }
}
