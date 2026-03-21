package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

import java.util.*;

import static shipwrights.genesis.teleportation.integration.Util.getSortedShips;

public class SpaceToPlanetTeleporter {
	private static final int LANDING_ACCURACY = 8; // Randomization range in chunks


	private final boolean gameTest;

	public SpaceToPlanetTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(TickEvent.LevelTickEvent event) {
		if (TickEvent.Phase.END.equals(event.phase) && event.level instanceof ServerLevel serverLevel && GenesisMod.isSpaceDimension(serverLevel)) {
			if (gameTest || !serverLevel.getPlayers(u -> true, 1).isEmpty()) {
				tick(serverLevel);
			}
		}
	}

	private static void tick(ServerLevel level) {
		long ticks = GenesisMod.getTicks(level);

		for (LoadedServerShip ship : getSortedShips(level)) {
			Vec3 shipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));
			AABBic shipAABB = ship.getShipAABB();

			Celestial nearest = getNearestPlanet(ship, ticks);
			if (ship.isStatic() || nearest == null || shipAABB == null) continue;

			PlayerTeam team = level.getScoreboard().getPlayerTeam(nearest.ID().getPath());
			if(team!=null)
			{
				Collection<String> teamShips = team.getPlayers();
				if(!teamShips.contains(ship.getSlug()))
				{
					continue;
				}
			}

			if (!shipOverlapsCelestial(ship, shipAABB, nearest, ticks)) continue;

			ServerLevel targetLevel = getTargetLevel(level, nearest);
			if (targetLevel == null) continue;

			Vector3d newPos = computePlanetTarget(level);
			Quaterniond rotation = getNewShipRot(shipCenter, nearest, ticks);

			DimensionTravelTeleporter.teleportShip(ship, TravelDirection.SPACE_TO_PLANET, level, targetLevel, newPos, rotation);
		}
	}

	private static boolean shipOverlapsCelestial(LoadedServerShip ship, AABBic shipAABB, Celestial nearest, long ticks) {
		return OBB.fromShip(shipAABB, ship.getShipToWorld()).overlapsWith(nearest.getOBB(ticks));
	}

	private static @Nullable ServerLevel getTargetLevel(ServerLevel level, Celestial nearest) {
		ResourceKey<Level> targetDimension = ResourceKey.create(
			net.minecraft.core.registries.Registries.DIMENSION,
			nearest.ID()
		);
        return level.getServer().getLevel(targetDimension);
	}

	private static Vector3d computePlanetTarget(ServerLevel level) {
		ChunkPos landingChunkPos = new ChunkPos(
			level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY,
			level.random.nextInt(LANDING_ACCURACY * 2 + 1) - LANDING_ACCURACY
		);
		return new Vector3d(
			SectionPos.sectionToBlockCoord(landingChunkPos.x),
			GenesisMod.atmosphereEntryHeight,
			SectionPos.sectionToBlockCoord(landingChunkPos.z)
		);
	}

	private static Quaterniond getNewShipRot(Vec3 shipCenter, Celestial nearest, long ticks) {
		Vector3dc planetPos = nearest.getPosition(ticks);
		Vector3d directionToPlanet = new Vector3d(
			shipCenter.x - planetPos.x(),
			shipCenter.y - planetPos.y(),
			shipCenter.z - planetPos.z()
		).normalize();
		Quaterniond rotation = new Quaterniond().rotateTo(new Vector3d(0, 1, 0), directionToPlanet);
		nearest.getRotation(ticks, 0f).mul(rotation, rotation).conjugate();
		return rotation;
	}

	@Nullable static Celestial getNearestPlanet(Ship ship, long ticks) {
		AABBic shipAABB = ship.getShipAABB();
		if (shipAABB != null) {
			OBB shipOBB = OBB.fromShip(shipAABB, ship.getShipToWorld());

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
