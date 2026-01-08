package shipwrights.genesis;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.event.AtmosphericCollision;
import shipwrights.genesis.event.PlanetCollision;

import java.util.List;

// TODO: this class should disappear, it looks like api but isn't, and also violates Single Responsibility Principle
@Mod.EventBusSubscriber
public class GenesisEvents {

	public static Component message = Component.empty();
	private static int wormholeLightningTimer = 0;

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

				if (serverLevel.dimension().location().equals(GenesisMod.WORMHOLE_DIM)) {
					wormholeLightningTick(serverLevel);
				}
			}
		}
	}

	/**
	 * Spawns lightning periodically in the Wormhole dimension near players.
	 * On average, spawns lightning once every 10 seconds (200 ticks).
	 */
	private static void wormholeLightningTick(ServerLevel wormholeLevel) {
		wormholeLightningTimer++;

		if (wormholeLightningTimer >= 20) {
			wormholeLightningTimer = 0;

			if (wormholeLevel.random.nextFloat() < 0.05f) {
				List<ServerPlayer> players = wormholeLevel.getPlayers(player -> true);
				if (!players.isEmpty()) {
					ServerPlayer randomPlayer = players.get(wormholeLevel.random.nextInt(players.size()));

					double distance = 30 + wormholeLevel.random.nextDouble() * 50;
					double angle = wormholeLevel.random.nextDouble() * Math.PI * 2;

					Vec3 playerPos = randomPlayer.position();
					double offsetX = Math.cos(angle) * distance;
					double offsetZ = Math.sin(angle) * distance;

					BlockPos lightningPos = new BlockPos(
						(int) (playerPos.x + offsetX),
						(int) playerPos.y + wormholeLevel.random.nextInt(-25, 25),
						(int) (playerPos.z + offsetZ)
					);

					LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(wormholeLevel);
					if (lightning != null) {
						lightning.moveTo(Vec3.atBottomCenterOf(lightningPos));
						lightning.setVisualOnly(true);
						wormholeLevel.addFreshEntity(lightning);
					}
				}
			}
		}
	}
}
