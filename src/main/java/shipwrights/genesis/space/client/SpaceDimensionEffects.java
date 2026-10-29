package shipwrights.genesis.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;

/// Space has no atmosphere: no sky, no clouds, and black fog, so celestials are seen against black.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpaceDimensionEffects extends DimensionSpecialEffects {

    public SpaceDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @SubscribeEvent
    public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(GenesisMod.SPACE, new SpaceDimensionEffects());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
    public static class FogColor {
        // Vanilla mixes the fog towards the biome's sky colour, so the fog colour above isn't enough on its own
        @SubscribeEvent
        public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.effects() instanceof SpaceDimensionEffects) {
                event.setRed(0);
                event.setGreen(0);
                event.setBlue(0);
            }
        }
    }
}
