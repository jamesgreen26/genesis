package shipwrights.genesis.event;

import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;
import shipwrights.genesis.ship.ShipLandingAttachment;
import shipwrights.genesis.teleportation.TeleportationHandler;
import shipwrights.genesis.util.PlanetUtil;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.genesis.util.VSUtils.getLoadedShipsInLevel;

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
		// Check if this is a planet dimension
		final PlanetData planet = PlanetUtil.getPlanetByDimension(level.dimension());
		if (planet == null) {
			return;
		}

		// Get the space dimension
		final ResourceKey<Level> targetDimension = PlanetUtil.getSpaceDimension();
		final ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
		if (targetLevel == null) {
			return;
		}

		final ResourceKey<Level> dimension = level.dimension();
		final Vec3 planetPos = new Vec3(planet.pos.x, planet.pos.y, planet.pos.z);
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
			final Vector3d targetPos = new Vector3d(0, planet.size / 2 + 120, 0);
			final Quaterniond rotation = PlanetUtil.getPlanetRotation(planet);
			rotation.transform(targetPos);
			targetPos.add(planetPos.x, planetPos.y, planetPos.z);

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
