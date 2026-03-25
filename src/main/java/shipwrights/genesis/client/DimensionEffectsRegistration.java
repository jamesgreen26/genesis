package shipwrights.genesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static shipwrights.genesis.GenesisMod.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DimensionEffectsRegistration {

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SPACE_DIM, new SpaceDimensionEffects());
        event.register(WORMHOLE_DIM, new WormholeDimensionEffects());
        event.register(GENERIC_PLANET_ID, new PlanetDimensionEffects());
    }
}
