package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import shipwrights.genesis.client.sound.AudioFilterManager;
import shipwrights.genesis.client.sound.SoundFilterHandler;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "loadLibrary", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;reset()V"))
    private void initAudioFilters(CallbackInfo ci) {
        AudioFilterManager.attemptInitialize();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void disposeAudioFilters(CallbackInfo ci) {
        AudioFilterManager.dispose();
    }

    @Inject(method = "play", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ))
    private void applyFilterOnPlay(
            SoundInstance sound,
            CallbackInfo ci,
            @Local(ordinal = 0) ChannelAccess.ChannelHandle channelHandle
    ) {
        final int targetFilterId = SoundFilterHandler.determineFilterId(sound);

        if (targetFilterId == AL10.AL_NONE)
            return;

        genesis$executeFilterApplication(channelHandle, targetFilterId);
    }

    @Inject(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private void updateFilterOnStateChange(
            CallbackInfo ci,
            @Local(ordinal = 0) ChannelAccess.ChannelHandle channelHandle,
            @Local SoundInstance soundInstance
    ) {
        final int targetFilterId = SoundFilterHandler.determineFilterId(soundInstance);
        genesis$executeFilterApplication(channelHandle, targetFilterId);
    }

    @Unique
    private void genesis$executeFilterApplication(ChannelAccess.ChannelHandle channelHandle, final int targetFilterId) {
        channelHandle.execute(channel -> {
            int sourceId = ((ChannelAccessor) channel).getSourceId();
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, targetFilterId);
        });
    }
}
