package shipwrights.genesis.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import shipwrights.genesis.GenesisMod;

public class SoundFilterHandler {
    private static final Minecraft client = Minecraft.getInstance();

    public static int determineFilterId(SoundInstance sound) {
        if (!AudioFilterManager.isReady() || client.player == null || client.level == null)
            return AL10.AL_NONE;

        if (shouldExcludeFromFiltering(sound)) {
            return AL10.AL_NONE;
        }

        return getSpaceFilterId();
    }

    private static int getWaterFilterId() {
        if (client.player == null)
            return AL10.AL_NONE;

        if (client.player.isUnderWater())
            return AudioFilterManager.getFilterId(AudioFilterManager.Filter.LOWPASS);

        return AL10.AL_NONE;
    }

    private static boolean shouldExcludeFromFiltering(SoundInstance sound) {
        return sound.getSource() == SoundSource.MUSIC ||
                sound.getSource() == SoundSource.WEATHER ||
                sound.getSource() == SoundSource.MASTER;
    }

    private static int getSpaceFilterId() {
        if (client.player == null)
            return AL10.AL_NONE;

        if (client.player.level().dimension().location().equals(GenesisMod.SPACE_DIM))
            return AudioFilterManager.getFilterId(AudioFilterManager.Filter.LOWPASS);

        return AL10.AL_NONE;
    }
}
