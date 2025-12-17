package shipwrights.genesis;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import shipwrights.genesis.event.AtmosphericCollision;
import shipwrights.genesis.event.PlanetCollision;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.SyncPlanetsPacket;

import java.util.List;

@Mod.EventBusSubscriber
public class GenesisEvents {

	public static Component message = Component.empty();
	private static int wormholeLightningTimer = 0;

	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SyncPlanetsPacket packet = SyncPlanetsPacket.fromPlanets(GenesisMod.planets.values());
			GenesisNetworking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
			GenesisMod.LOGGER.info("Sent {} planets to {}", GenesisMod.planets.size(), player.getName().getString());
		}
	}

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
