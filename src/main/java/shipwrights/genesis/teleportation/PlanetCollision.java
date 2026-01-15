package shipwrights.genesis.teleportation;

import kotlin.Pair;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;
import shipwrights.genesis.space.type.CelestialType;

import java.util.*;

import static shipwrights.genesis.teleportation.VSUtils.getLoadedShipsInLevel;

public class PlanetCollision {
	private static final Logger LOGGER = GenesisMod.LOGGER;

	private static final double OUTER_RANGE = 128;
	private static final double CLOSE_RANGE = 16;
	private static final int LANDING_ACCURACY = 8; // Randomization range in chunks

	public static void planetCollisionTick(final ServerLevel level) {
		long ticks = GenesisMod.getTicks(level);

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

			Celestial nearest = getNearest(ship, ticks);
			if (nearest == null) continue;

			// Find nearest planet
			final Pair<Celestial, Double> nearestPlanetData = SpaceLevel.nearestCelestialWhere(new Vector3d(shipCenter.x, shipCenter.y, shipCenter.z), GenesisMod.getTicks(level), 0f, CelestialType::isVisitable);
			if (nearestPlanetData == null) {
				continue;
			}

            final double distance = Math.sqrt(nearestPlanetData.getSecond());
			double closeRange = nearest.getActualSize() / 8;

			final ResourceKey<Level> targetDimension = ResourceKey.create(
				net.minecraft.core.registries.Registries.DIMENSION,
				nearest.getID()
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
			if (distance > nearest.getActualSize()) {
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

			final Vector3dc planetPos = nearest.getPosition(ticks);

			// Calculate rotation based on planet position
			final Vector3d directionToPlanet = new Vector3d(
				shipCenter.x - planetPos.x(),
				shipCenter.y - planetPos.y(),
				shipCenter.z - planetPos.z()
			).normalize();
			final Quaterniond rotation = new Quaterniond().rotateTo(new Vector3d(0, 1, 0), directionToPlanet);
			final Quaterniondc planetRotation = nearest.getRotation(ticks, 0f);
			planetRotation.mul(rotation, rotation).conjugate();

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

	@Nullable static Celestial getNearest(Ship ship, long ticks) {
		final AABBic shipAABB = ship.getShipAABB();
		if (shipAABB != null) {
			final OBB shipOBB = OBB.fromShip(shipAABB, ship.getShipToWorld());

			Optional<Celestial> nearest =
					GenesisMod.SPACE_REGISTRY
							.getWhere(CelestialType::isVisitable)
							.stream()
							.map(c -> Map.entry(c, c.getOBB(ticks).distanceTo(shipOBB)))
							.min(Comparator.comparingDouble(Map.Entry::getValue))
							.map(Map.Entry::getKey);
			if (nearest.isPresent()) {
				return nearest.get();
			}
		}
		return null;
	}
}
