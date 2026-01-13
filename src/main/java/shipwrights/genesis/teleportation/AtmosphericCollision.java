package shipwrights.genesis.teleportation;

import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.OrbitingBody;

import static shipwrights.genesis.teleportation.VSUtils.getLoadedShipsInLevel;

public class AtmosphericCollision {
	private static final Logger LOGGER = GenesisMod.LOGGER;

	private static final TeleportationHandler TELEPORT_HANDLER = new TeleportationHandler(null, null, false);

	/**
	 * Checks all VS ships for the given level, if any of them are above their
	 * dimensions atmosphere, they will be moved to space.
	 *
	 * @param level
	 */
	public static void atmosphericCollisionTick(final ServerLevel level) {
		// Check if this is a body dimension
		final OrbitingBody body = GenesisMod.getDataForLevel(level);
		if (body == null) {
			return;
		}

		final ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM);
		final ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
		if (targetLevel == null) {
			return;
		}

		final ResourceKey<Level> dimension = level.dimension();
		final Vector3dc planetPos = body.getCurrentPos(GenesisMod.getTicks(level));
		final double atmoHeight = GenesisMod.atmosphereExitHeight;

		final TeleportationHandler teleportHandler = TELEPORT_HANDLER;
		teleportHandler.reset(level, targetLevel);

		for (final LoadedServerShip ship : getLoadedShipsInLevel(level)) {
			if (ship.isStatic() || teleportHandler.hasShip(ship)) {
				continue;
			}
			final Vector3dc shipPos = ship.getTransform().getPositionInWorld();
			final double shipY = shipPos.y();
			final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);
			if (shipY + 10 < atmoHeight) {
				landingAttachment.landing = false;
				continue;
			}
			if (landingAttachment.landing && shipY < atmoHeight + 128) {
				continue;
			}

			// Calculate target position in space
			final Vector3d targetPos = new Vector3d(0, body.getActualSize() * 0.7 + 20, 0);
			final Quaterniondc rotation = body.getRotation(GenesisMod.getTicks(level));
			rotation.transform(targetPos);
			targetPos.add(planetPos.x(), planetPos.y(), planetPos.z());

			MinecraftForge.EVENT_BUS.post(new PreTravelEvent.PlanetToSpace(dimension, shipPos, targetDimension, targetPos, rotation));

			LOGGER.info("[genesis]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDimension.location(), targetPos.x, targetPos.y, targetPos.z);
			teleportHandler.addShip(ship, targetPos, rotation);
		}
		for (final LoadedServerShip ship : teleportHandler.getPendingShips()) {
			final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);
			final Vector3dc pos = ship.getTransform().getPositionInWorld();
			landingAttachment.setLaunching(dimension, new ChunkPos(SectionPos.blockToSectionCoord(pos.x()), SectionPos.blockToSectionCoord(pos.z())));
		}
		teleportHandler.finalizeTeleport();
	}
}
