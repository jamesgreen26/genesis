package shipwrights.genesis.client.sound;

import org.lwjgl.openal.*;
import shipwrights.genesis.GenesisMod;

import java.util.EnumMap;
import java.util.Map;

public class AudioFilterManager {

    public enum Filter {
        LOWPASS,
        HIGHPASS,
        BANDPASS
    }

    private static final Map<Filter, Integer> FILTER_IDS = new EnumMap<>(Filter.class);

    private static boolean efxSupported = false;
    private static boolean attemptedInit = false;

    public static void attemptInitialize() {
        if (attemptedInit) return;
        attemptedInit = true;

        if (!checkEfxSupport()) return;

        initializeLowPassFilter();

        if (FILTER_IDS.isEmpty())
            efxSupported = false;
    }

    private static void initializeLowPassFilter() {
        int filterId = EXTEfx.alGenFilters();

        if (hasAlError("Failed to generate EFX Filter for LPF."))
            return;

        try {
            EXTEfx.alFilteri(filterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(filterId, EXTEfx.AL_LOWPASS_GAINHF, 0.005f);

            if (hasAlError("Failed to set LPF parameters.")) {
                EXTEfx.alDeleteFilters(filterId);
            } else {
                FILTER_IDS.put(Filter.LOWPASS, filterId);
            }
        } catch (Exception e) {
            EXTEfx.alDeleteFilters(filterId);
        }
    }

    private static boolean checkEfxSupport() {
        long context = ALC10.alcGetCurrentContext();
        if (context == 0) return false;

        long device = ALC10.alcGetContextsDevice(context);
        ALCCapabilities caps = device == 0 ? null : ALC.getCapabilities();

        if (caps != null && caps.ALC_EXT_EFX) {
            efxSupported = true;
            return true;
        }
        return false;
    }

    private static boolean hasAlError(String errorContext) {
        int alError = AL10.alGetError();
        if (alError != AL10.AL_NO_ERROR) {
            GenesisMod.LOGGER.error("{}: Error code: {}", errorContext, alError);
            return true;
        }
        return false;
    }

    public static boolean isReady() {
        return efxSupported && !FILTER_IDS.isEmpty();
    }

    public static int getFilterId(Filter type) {
        return FILTER_IDS.getOrDefault(type, AL10.AL_NONE);
    }

    public static void dispose() {
        for (int filterId : FILTER_IDS.values())
            EXTEfx.alDeleteFilters(filterId);
        FILTER_IDS.clear();
        efxSupported = false;
        attemptedInit = false;
    }
}
