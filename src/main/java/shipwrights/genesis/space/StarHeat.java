package shipwrights.genesis.space;

import g_mungus.zps.gas.api.HeatSource;
import g_mungus.zps.gas.api.HeatSources;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Heats space around celestials with a {@linkplain Celestial#surfaceTemperature() surface temperature}, such as the
/// sun, through ZPS's heat sources. ZPS only heats biomes using its {@code zps:space} temperature modifier, as
/// {@code genesis:space} does.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StarHeat {

    private StarHeat() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        HeatSources.register(StarHeat::sources);
    }

    /// In space, celestial positions are world coordinates, so they're used as they are
    private static Collection<HeatSource> sources(Level level) {
        if (!GenesisMod.isSpaceDimension(level)) return List.of();
        Registry<Celestial> celestials = level.registryAccess().registry(Celestial.REGISTRY_KEY).orElse(null);
        if (celestials == null) return List.of();

        Time time = Time.get(level);
        List<HeatSource> sources = new ArrayList<>();
        for (Celestial celestial : celestials) {
            // A heat source needs a positive radius; without a size, there's no surface to heat from
            if (celestial.surfaceTemperature() <= 0 || !(celestial.size() > 0)) continue;
            Vector3dc position = celestial.transformProvider().getPosition(celestials, time);
            sources.add(new HeatSource(new Vec3(position.x(), position.y(), position.z()),
                    celestial.surfaceTemperature(), celestial.size() / 2));
        }
        return sources;
    }
}
