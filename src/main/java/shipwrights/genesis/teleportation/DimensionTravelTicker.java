package shipwrights.genesis.teleportation;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DimensionTravelTicker {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLevelTick(final TickEvent.LevelTickEvent event) {
        if (TickEvent.Phase.END.equals(event.phase) && event.level instanceof ServerLevel serverLevel) {

            if (!(serverLevel.getPlayers(u -> true, 1).isEmpty())) {
                AtmosphericCollision.atmosphericCollisionTick(serverLevel);
                PlanetCollision.planetCollisionTick(serverLevel);
            }
        }
    }
}
