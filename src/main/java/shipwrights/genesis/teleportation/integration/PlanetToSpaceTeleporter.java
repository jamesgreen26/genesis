package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

import static shipwrights.genesis.teleportation.integration.Util.getSortedShips;

public class PlanetToSpaceTeleporter {
	private final boolean gameTest;

	public PlanetToSpaceTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(final TickEvent.LevelTickEvent event) {
		if (TickEvent.Phase.END.equals(event.phase) && event.level instanceof ServerLevel serverLevel) {
			if (gameTest || !serverLevel.getPlayers(u -> true, 1).isEmpty()) {
				tick(serverLevel);
			}
		}
	}

	private static void tick(final ServerLevel level) {
		final Celestial body = GenesisMod.getCelestialForLevel(level);
		final ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));

		if (body == null || spaceLevel == null) {
			return;
		}

		final long ticks = GenesisMod.getTicks(level);
		final Vector3dc planetPos = body.getPosition(ticks);
		final Quaterniondc rotation = body.getRotation(ticks);
		final Vector3d spaceTarget = computeSpaceTarget(body, planetPos, rotation);

		for (final LoadedServerShip ship : getSortedShips(level)) {
			if (!ship.isStatic() && ship.getTransform().getPositionInWorld().y() > GenesisMod.atmosphereExitHeight) {
				DimensionTravelTeleporter.teleportShip(
						ship,
						TravelDirection.PLANET_TO_SPACE,
						level,
						spaceLevel,
						spaceTarget,
						rotation
				);
			}
		}
	}

	private static Vector3d computeSpaceTarget(final Celestial body, final Vector3dc planetPos, final Quaterniondc rotation) {
		final Vector3d targetPos = new Vector3d(0, body.getActualSize() + 20, 0);
		rotation.transform(targetPos);
		targetPos.add(planetPos.x(), planetPos.y(), planetPos.z());
		return targetPos;
	}
}
