package shipwrights.genesis.content.crawler;

import g_mungus.zps.gas.api.GasEnvironmentEvent;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.GenesisEntityTypes;

@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class MoonCrawlerEvents {
    private MoonCrawlerEvents() {
    }

    /// Moon crawlers live in a vacuum: ZPS shouldn't suffocate, freeze or burn them
    @SubscribeEvent
    public static void onGasEnvironment(GasEnvironmentEvent event) {
        if (event.getEntity() instanceof MoonCrawler) {
            event.getEnvironment().setBreathable(true);
            event.getEnvironment().setExposed(false);
        }
    }

    @Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onAttributeCreation(EntityAttributeCreationEvent event) {
            event.put(GenesisEntityTypes.MOON_CRAWLER.get(), MoonCrawler.createAttributes().build());
        }

        @SubscribeEvent
        public static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
            event.register(GenesisEntityTypes.MOON_CRAWLER.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoonCrawler::checkMoonCrawlerSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
