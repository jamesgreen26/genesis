package shipwrights.genesis.space.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;

/// The surface of an airless celestial, such as the moon: a normal sky, so celestials are drawn as they are from the
/// overworld, but without clouds or a sunrise glow. The sky's colour comes from the dimension's biomes.
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public class PlanetDimensionEffects extends DimensionSpecialEffects {

    public PlanetDimensionEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @SubscribeEvent
    public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(GenesisMod.id("planet"), new PlanetDimensionEffects());
    }

    /// As in the overworld, darkening with the daylight
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    /// No air to scatter the light into a sunrise
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }
}
