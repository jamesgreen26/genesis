package shipwrights.genesis;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.event.AtmosphericCollision;
import shipwrights.genesis.event.PlanetCollision;

@Mod.EventBusSubscriber
public class GenesisEvents {

	public static Component message = Component.empty();

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLevelTick(final TickEvent.LevelTickEvent event) {
		if (!(event.level instanceof ServerLevel serverLevel)) {
			return;
		}
		switch (event.phase) {
			case START -> {
			}
			case END -> {
				if (serverLevel.getPlayers(player -> true, 1).isEmpty()) {
					// Skip if no player is in the world
					return;
				}
				AtmosphericCollision.atmosphericCollisionTick(serverLevel);
				PlanetCollision.planetCollisionTick(serverLevel);
			}
		}
		//uncomment for debug display message
//		event.level.getServer().getPlayerList().getPlayers().forEach(serverPlayerEntity -> {
//			serverPlayerEntity.displayClientMessage(message, true);
//
//		});
	}
}
