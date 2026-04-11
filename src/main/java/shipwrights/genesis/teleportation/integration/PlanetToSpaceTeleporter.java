package shipwrights.genesis.teleportation.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Quaterniondc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

import static shipwrights.genesis.teleportation.integration.Util.getSortedShips;

public class PlanetToSpaceTeleporter {
	private final boolean gameTest;

	public PlanetToSpaceTeleporter(boolean gameTest) {
		this.gameTest = gameTest;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLevelTick(TickEvent.LevelTickEvent event) {
		if (TickEvent.Phase.END.equals(event.phase) && event.level instanceof ServerLevel serverLevel) {
			if (gameTest || !serverLevel.getPlayers(u -> true, 1).isEmpty()) {
				tick(serverLevel);
			}
		}
	}

	private static void tick(ServerLevel level) {
		Celestial body = GenesisMod.getCelestialForLevel(level);
		ServerLevel spaceLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM));

		if (body == null || spaceLevel == null) {
			return;
		}

		long ticks = GenesisMod.getTicks(level);

		for (LoadedServerShip ship : getSortedShips(level)) {
			Vector3dc shipPos = ship.getTransform().getPositionInWorld();
			if (!ship.isStatic() && shipPos.y() > GenesisCommonConfig.getAtmosphereExitHeight()) {


				if (VantagePoint.get(level, shipPos, ticks, 0f) instanceof VantagePoint.OnCelestial vantagePoint) {
					DimensionTravelTeleporter.teleportShip(
							ship,
							TravelDirection.PLANET_TO_SPACE,
							level,
							spaceLevel,
							computeSpaceTarget(vantagePoint),
							vantagePoint.getCelestialRotation().mul(vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()), new Quaterniond())
					);
				}
			}
		}
	}

    // Package-private — accessed by tests
	static Quaterniondc computeSpaceRotation(Quaterniondc vantageRotation, Quaterniondc shipRotation) {
		return new Quaterniond(vantageRotation).mul(shipRotation, new Quaterniond());
	}

	private static Vector3d computeSpaceTarget(VantagePoint.OnCelestial vantagePoint) {
		Vector3d targetPos = new Vector3d(0, vantagePoint.celestial().getActualSize() + 20, 0);
		vantagePoint.cameraRotationFromNorthPole().conjugate(new Quaterniond()).transform(targetPos);
		vantagePoint.getCelestialRotation().transform(targetPos);
		targetPos.add(vantagePoint.getPosition());
		return targetPos;
    }
}
