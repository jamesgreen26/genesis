package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.sound.GenesisSounds;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class WormholeAmbianceHandler {
    private static SimpleSoundInstance voidEngineStartSound = null;
    private static SimpleSoundInstance currentAmbianceSound = null;
    private static SimpleSoundInstance fadingOutSound = null;
    private static boolean wasInWormhole = false;
    private static int soundDurationTicks = 0;
    private static int currentTick = 0;
    private static int fadeOutTicksRemaining = 0;
    private static final int CROSSFADE_DURATION_TICKS = 40; // 2 seconds at 20 ticks/sec

    public static BlockPos wormholeTravelPos = new BlockPos(0,0,0);
    public static BlockPos voidEngineStartPos = new BlockPos(0,0,0);

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAmbiance();
            wasInWormhole = false;
            currentTick = 0;
            return;
        }

        boolean isInWormhole = minecraft.level.dimension().location().equals(GenesisMod.WORMHOLE_DIM);

        if (isInWormhole && !wasInWormhole) {
            // Just entered wormhole dimension
            startAmbiance(minecraft.getSoundManager());

            playTravelSound(minecraft);
        } else if (!isInWormhole && wasInWormhole) {
            // Just left wormhole dimension
            stopAmbiance();
            playTravelSound(minecraft);
            SimpleSoundInstance stopSound = new SimpleSoundInstance(
                    GenesisSounds.VOID_ENGINE_STOP.get().getLocation(),
                    SoundSource.BLOCKS,
                    1.0f,
                    1.0f,
                    Minecraft.getInstance().player.getRandom(),
                    false,
                    0,
                    SimpleSoundInstance.Attenuation.LINEAR,
                    wormholeTravelPos.getX(), wormholeTravelPos.getY(), wormholeTravelPos.getZ(),
                    true
            );
            minecraft.getSoundManager().play(stopSound);

            currentTick = 0;
        } else if (isInWormhole) {
            currentTick++;

            // Handle fading out the old sound
            if (fadeOutTicksRemaining > 0) {
                fadeOutTicksRemaining--;
                if (fadeOutTicksRemaining == 0 && fadingOutSound != null) {
                    minecraft.getSoundManager().stop(fadingOutSound);
                    fadingOutSound = null;
                }
            }

            // Start crossfade before the sound ends
            if (currentAmbianceSound != null &&
                soundDurationTicks > 0 &&
                currentTick >= soundDurationTicks - CROSSFADE_DURATION_TICKS &&
                fadingOutSound == null) {
                startCrossfade(minecraft.getSoundManager());
            }

            // Check if current sound finished and we didn't crossfade (fallback)
            if (currentAmbianceSound != null && !minecraft.getSoundManager().isActive(currentAmbianceSound)) {
                startAmbiance(minecraft.getSoundManager());
            }
        }

        wasInWormhole = isInWormhole;
    }

    private static void startCrossfade(SoundManager soundManager) {
        // Move current sound to fading out
        fadingOutSound = currentAmbianceSound;
        fadeOutTicksRemaining = CROSSFADE_DURATION_TICKS;

        // Start new sound
        currentAmbianceSound = new SimpleSoundInstance(
            GenesisSounds.WORMHOLE_AMBIANCE.get().getLocation(),
            SoundSource.AMBIENT,
            1.0f,
            1.0f,
            Minecraft.getInstance().player.getRandom(),
            false, // Not looping - we handle the loop manually for crossfade
            0,
            SimpleSoundInstance.Attenuation.NONE,
            0.0, 0.0, 0.0,
            true
        );

        soundManager.play(currentAmbianceSound);
        currentTick = 0;
    }

    private static void startAmbiance(SoundManager soundManager) {
        stopAmbiance(); // Stop any existing sound first

        currentAmbianceSound = new SimpleSoundInstance(
            GenesisSounds.WORMHOLE_AMBIANCE.get().getLocation(),
            SoundSource.AMBIENT,
            1.0f,
            1.0f,
            Minecraft.getInstance().player.getRandom(),
            false, // Not looping - we handle the loop manually for crossfade
            0,
            SimpleSoundInstance.Attenuation.NONE,
            0.0, 0.0, 0.0,
            true
        );

        soundManager.play(currentAmbianceSound);
        currentTick = 0;

        // Estimate sound duration (you may need to adjust this based on your actual sound file length)
        // This is in ticks (20 ticks = 1 second)
        soundDurationTicks = estimateSoundDuration();
    }

    private static int estimateSoundDuration() {
        return 1900;
    }

    private static void stopAmbiance() {
        if (currentAmbianceSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentAmbianceSound);
            currentAmbianceSound = null;
        }
        if (fadingOutSound != null) {
            Minecraft.getInstance().getSoundManager().stop(fadingOutSound);
            fadingOutSound = null;
        }
    }

    private static void playTravelSound(Minecraft minecraft) {
        SimpleSoundInstance travelSound = new SimpleSoundInstance(
                GenesisSounds.VOID_ENGINE_TRAVEL.get().getLocation(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f,
                Minecraft.getInstance().player.getRandom(),
                false,
                0,
                SimpleSoundInstance.Attenuation.LINEAR,
                wormholeTravelPos.getX(), wormholeTravelPos.getY(), wormholeTravelPos.getZ(), //get block pos from packet
                true
        );
        minecraft.getSoundManager().play(travelSound);
    }

    public static void playVoidEngineStart() {
        if (voidEngineStartSound == null) {
            voidEngineStartSound = new SimpleSoundInstance(
                    GenesisSounds.VOID_ENGINE_START.get().getLocation(),
                    SoundSource.BLOCKS,
                    0.5f,
                    1.0f,
                    Minecraft.getInstance().player.getRandom(),
                    false,
                    0,
                    SimpleSoundInstance.Attenuation.LINEAR,
                    0.0, 0.0, 0.0, //get block pos from packet
                    true
            );
        }
        Minecraft.getInstance().getSoundManager().play(voidEngineStartSound);
    }

    public static void stopVoidEngineStart() {
        Minecraft.getInstance().getSoundManager().stop(voidEngineStartSound);
    }
}
