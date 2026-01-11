package shipwrights.genesis.teleportation;

import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.SpaceLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static shipwrights.genesis.teleportation.VSUtils.getLoadedShipsInLevel;

public class PlanetCollision {
	private static final Logger LOGGER = GenesisMod.LOGGER;

	private static final double OUTER_RANGE = 128;
	private static final double CLOSE_RANGE = 16;
	private static final int LANDING_ACCURACY = 8; // Randomization range in chunks

	public static void planetCollisionTick(final ServerLevel level) {
		// Only run in space dimension
		if (!GenesisMod.isSpaceDimension(level)) {
			return;
		}

		final Map<ResourceKey<Level>, TeleportationHandler> handlers = new HashMap<>();

		final List<LoadedServerShip> ships = getLoadedShipsInLevel(level);
		// Sort by volume (largest first) to handle nested ships correctly
		ships.sort((a, b) -> {
			final AABBdc aBox = a.getWorldAABB();
			final AABBdc bBox = b.getWorldAABB();
			final double n =
				(aBox.maxX() - aBox.minX()) * (aBox.maxY() - aBox.minY()) * (aBox.maxZ() - aBox.minZ())
				- (bBox.maxX() - bBox.minX()) * (bBox.maxY() - bBox.minY()) * (bBox.maxZ() - bBox.minZ());
			if (n < 0) {
				return 1;
			}
			if (n > 0) {
				return -1;
			}
			return Long.compare(a.getId(), b.getId());
		});

		for (final LoadedServerShip ship : ships) {
			final Vec3 shipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));

			// Find nearest planet
			final OrbitingBody.WithDistanceSq nearestPlanetData = SpaceLevel.getNearestOrbitingBody(new Vector3d(shipCenter.x, shipCenter.y, shipCenter.z), level.getGameTime()).orElse(null);
			if (nearestPlanetData == null) {
				continue;
			}

			final OrbitingBody planet = nearestPlanetData.getCelestial();
			final double distance = Math.sqrt(nearestPlanetData.getDistanceSquared());
			double closeRange = planet.getActualSize() / 8;

			final ResourceKey<Level> targetDimension = ResourceKey.create(
				net.minecraft.core.registries.Registries.DIMENSION,
				planet.getID()
			);
			final ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
			if (targetLevel == null) {
				continue;
			}

			// Check if already being handled
			{
				final TeleportationHandler handler = handlers.get(targetDimension);
				if (handler != null && handler.hasShip(ship)) {
					continue;
				}
			}

			final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);

			// Too far away
			if (distance > planet.getActualSize()) {
				landingAttachment.launching = false;
				continue;
			}

			// Still launching
			if (landingAttachment.launching && distance > 0) {
				continue;
			}

			// Ignore static ships unless they're frozen (being landed)
			if (!landingAttachment.frozen && ship.isStatic()) {
				continue;
			}

			// Freeze ship if close enough and not static
			if (!ship.isStatic() && distance <= closeRange) {
				landingAttachment.freezeShip(ship);
			}

			// Use (0, atmoHeight, 0) as landing position with some randomization
			final ChunkPos landingChunkPos = new ChunkPos(
				level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY,
				level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY
			);

			final Vector3d newPos = new Vector3d(
				SectionPos.sectionToBlockCoord(landingChunkPos.x),
				GenesisMod.atmosphereEntryHeight,
				SectionPos.sectionToBlockCoord(landingChunkPos.z)
			);

			final long ticks = level.getGameTime();
			final Vector3dc planetPos = planet.getCurrentPos(ticks);

			// Calculate rotation based on planet position
			final Vector3d directionToPlanet = new Vector3d(
				shipCenter.x - planetPos.x(),
				shipCenter.y - planetPos.y(),
				shipCenter.z - planetPos.z()
			).normalize();
			final Quaterniond rotation = new Quaterniond().rotateTo(new Vector3d(0, 1, 0), directionToPlanet);
			final Quaterniondc planetRotation = planet.getRotation(ticks, 0f);
			planetRotation.mul(rotation, rotation).conjugate();

			MinecraftForge.EVENT_BUS.post(new PreTravelEvent.SpaceToPlanet(
				level.dimension(),
				ship.getTransform().getPositionInWorld(),
				targetDimension,
				newPos,
				rotation
			));

			LOGGER.info("[genesis]: Handling teleport {} ({}) to {} {} {} {}",
				ship.getSlug(), ship.getId(), targetDimension.location(), newPos.x, newPos.y, newPos.z);

			final TeleportationHandler handler = handlers.computeIfAbsent(
				targetDimension,
				(targetDim1) -> new TeleportationHandler(level, targetLevel, true)
			);
			handler.addShip(ship, newPos, rotation);
		}

		for (final TeleportationHandler handler : handlers.values()) {
			for (final LoadedServerShip ship : handler.getPendingShips()) {
				final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
				attachment.frozen = false;
				attachment.setLanding();
			}
			handler.finalizeTeleport();
		}
	}
}
