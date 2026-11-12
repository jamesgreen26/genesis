package shipwrights.genesis.content.crawler;

import g_mungus.zps.gas.api.GasEnvironmentEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.GenesisEntityTypes;

@EventBusSubscriber(modid = GenesisMod.MOD_ID)
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

    @EventBusSubscriber(modid = GenesisMod.MOD_ID)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onAttributeCreation(EntityAttributeCreationEvent event) {
            event.put(GenesisEntityTypes.MOON_CRAWLER.get(), MoonCrawler.createAttributes().build());
        }

        @SubscribeEvent
        public static void onSpawnPlacementRegister(RegisterSpawnPlacementsEvent event) {
            event.register(GenesisEntityTypes.MOON_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MoonCrawler::checkMoonCrawlerSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
