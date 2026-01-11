package shipwrights.genesis.teleportation;

import g_mungus.vlib.dimension.DimensionSettingsManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.core.api.events.PhysTickEvent;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;


import java.util.*;

@Mod.EventBusSubscriber
public class TeleportationHandler {

	private static final Logger LOGGER = GenesisMod.LOGGER;

	private static final double ENTITY_COLLECT_RANGE = 8;
	private static final double SHIP_COLLECT_RANGE = 10;

	private static Map<Long, Set<Integer>> SHIP2CONSTRAINTS = Map.of();
	private static Map<Integer, VSJoint> ID2CONSTRAINT;

	private final Long2ObjectOpenHashMap<TeleportData> ships = new Long2ObjectOpenHashMap<>();
	private final Map<Entity, Vec3> entityToPos = new HashMap<>();
	private VsiServerShipWorld shipWorld;
	private double greatestOffset;
	private ServerLevel oldLevel;
	private ServerLevel newLevel;
	private final boolean isReturning;

	public TeleportationHandler(final ServerLevel oldLevel, final ServerLevel newLevel, final boolean isReturning) {
		this.shipWorld = newLevel == null ? null : VSGameUtilsKt.getShipObjectWorld(newLevel);
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		// Look for the lowest ship when escaping, in order to not collide with the planet.
		// Look for the highest ship when reentering, in order to not collide with the atmosphere.
		this.isReturning = isReturning;
	}

	@SubscribeEvent
	public static void onServerStart(final ServerStartedEvent event) {
	}

	public static void onPhysTick(PhysTickEvent event) {
		VsiPhysLevel level = (VsiPhysLevel) event.getWorld();
		if (SHIP2CONSTRAINTS.isEmpty()) {
			SHIP2CONSTRAINTS = level.getJointsByShipIds();
			ID2CONSTRAINT = level.getAllJoints();
		}

	}

	public void reset(final ServerLevel oldLevel, final ServerLevel newLevel) {
		this.shipWorld = newLevel == null ? null : VSGameUtilsKt.getShipObjectWorld(newLevel);
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.ships.clear();
		this.entityToPos.clear();
	}

	public boolean hasShip(final ServerShip ship) {
		return this.ships.containsKey(ship.getId());
	}

	public void addShip(final ServerShip ship, final Vector3dc newPos, final Quaterniondc rotation) {
		this.addShipWithVelocity(ship, newPos, rotation, null, null);
	}

	public void addShipWithVelocity(final ServerShip ship, final Vector3dc newPos, final Quaterniondc rotation, final Vector3dc velocity, final Vector3dc omega) {
		final long shipId = ship.getId();
		if (this.ships.containsKey(shipId)) {
			return;
		}
		this.greatestOffset = 0;
		final List<ServerShip> collected = new ArrayList<>();
		final Vector3dc origin = ship.getTransform().getPositionInWorld();
		this.collectShipAndConnectedWithVelocity(shipId, origin, newPos, rotation, velocity, omega, collected);
		this.collectNearbyShips(collected, origin, newPos, rotation);
		this.collectNearbyEntities(collected, origin, newPos, rotation);
		this.finalizeCollect(collected, rotation);
	}

	public List<LoadedServerShip> getPendingShips() {
		final List<LoadedServerShip> ships = new ArrayList<>(this.ships.size());
		for (long id : this.ships.keySet()) {
			final LoadedServerShip ship = this.shipWorld.getLoadedShips().getById(id);
			if (ship != null) {
				ships.add(ship);
			}
		}
		return ships;
	}

	private void collectShipAndConnected(final long shipId, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation, final List<ServerShip> collected) {
		this.collectShipAndConnectedWithVelocity(shipId, origin, newPos, rotation, null, null, collected);
	}

	private void collectShipAndConnectedWithVelocity(
		final long shipId,
		final Vector3dc origin,
		final Vector3dc newPos,
		final Quaterniondc rotation,
		Vector3dc velocity,
		Vector3dc omega,
		final List<ServerShip> collected
	) {
		if (this.ships.containsKey(shipId)) {
			return;
		}
		final ServerShip ship = this.getShip(shipId);
		if (!(ship instanceof LoadedServerShip)) {
			return;
		}
		final LoadedServerShip loadedShip = (LoadedServerShip) ship;
		final Vector3dc pos = loadedShip.getTransform().getPositionInWorld();
		final ShipLandingAttachment landingAttachment = loadedShip.getAttachment(ShipLandingAttachment.class);
		if (loadedShip.isStatic() && landingAttachment != null && landingAttachment.frozen) {
			velocity = landingAttachment.velocity;
			omega = landingAttachment.omega;
		} else {
			if (velocity == null) {
				velocity = new Vector3d(loadedShip.getVelocity());
			}
			if (omega == null) {
				omega = new Vector3d(loadedShip.getAngularVelocity());
			}
		}
		collected.add(loadedShip);

		final Vector3d relPos = pos.sub(origin, new Vector3d());
		final Quaterniond newRotataion = new Quaterniond(loadedShip.getTransform().getShipToWorldRotation());

		if (!this.isReturning) {
			final double offset = relPos.y;
			if (offset < this.greatestOffset) {
				this.greatestOffset = offset;
			}
		}

		rotation.transform(relPos);
		velocity = rotation.transform(velocity, new Vector3d());
		newRotataion.mul(rotation).normalize();

		if (this.isReturning) {
			final double offset = relPos.y;
			if (offset > this.greatestOffset) {
				this.greatestOffset = offset;
			}
		}

		relPos.add(newPos);
		final Vector3d velocity0 = new Vector3d(velocity);
		final Vector3d omega0 = new Vector3d(omega);

		// When entering space (not returning), scale velocity down!!
		if (!this.isReturning) {
			velocity0.mul(0.0625);
		}

		MinecraftForge.EVENT_BUS.post(this.createPreShipTravelEvent(
				loadedShip, oldLevel.dimension(), newLevel.dimension(), relPos, newRotataion, velocity0, omega0
		));

		this.ships.put(
			shipId,
			new TeleportData(
				relPos,
				newRotataion,
				velocity0,
				omega0
			)
		);

		try {
			final Set<Integer> constraints = SHIP2CONSTRAINTS.get(shipId);
			if (constraints != null) {
				constraints.stream().map(ID2CONSTRAINT::get).forEach((constraint) -> {
					this.collectShipAndConnected(constraint.getShipId0(), origin, newPos, rotation, collected);
					this.collectShipAndConnected(constraint.getShipId1(), origin, newPos, rotation, collected);
				});
			}
		} catch (ConcurrentModificationException e) {
			//fuck.
		}
	}

	private void collectNearbyShips(final List<ServerShip> collected, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		final QueryableShipData<LoadedServerShip> loadedShips = this.shipWorld.getLoadedShips();
		final Vector3d offset = newPos.sub(origin, new Vector3d());
		// Note: collected list will grow during the loop
		for (int i = 0; i < collected.size(); i++) {
			final AABBdc shipBox = collected.get(i).getWorldAABB();
			final AABBd box = new AABBd(
				shipBox.minX() - SHIP_COLLECT_RANGE, shipBox.minY() - SHIP_COLLECT_RANGE, shipBox.minZ() - SHIP_COLLECT_RANGE,
				shipBox.maxX() + SHIP_COLLECT_RANGE, shipBox.maxY() + SHIP_COLLECT_RANGE, shipBox.maxZ() + SHIP_COLLECT_RANGE);
			for (final ServerShip ship : loadedShips.getIntersecting(box)) {
				this.collectShipAndConnected(ship.getId(), origin, newPos, rotation, collected);
			}
		}
	}

	private void collectNearbyEntities(final List<ServerShip> collected, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		for (final ServerShip ship : collected) {
			this.collectEntities(ship, origin, newPos, rotation);
		}
	}

	private void finalizeCollect(final List<ServerShip> collected, final Quaterniondc rotation) {
		final Vector3d offset = new Vector3d(0, -this.greatestOffset, 0);
		if (!this.isReturning) {
			rotation.transform(offset);
		}
		for (final ServerShip ship : collected) {
			final long id = ship.getId();
			final Vector3d newPos = this.ships.get(id).newPos();
			newPos.add(offset);
		}
	}

	public void finalizeTeleport() {
		final int size = this.ships.size();
		if (size == 0) {
			return;
		}
		this.ships.forEach(this::handleShipTeleport);
		this.ships.clear();
		if (size >= 256) {
			this.ships.trim(32);
		}
		this.teleportEntities();
	}

	private void collectEntities(final ServerShip ship, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		// Entities in range
		final AABBd shipBoxd = new AABBd(ship.getWorldAABB());
		final AABBic shipBoxi = ship.getShipAABB();
		if (shipBoxi != null) {
			final AABBd shipYardBox = new AABBd(
				shipBoxi.minX(), shipBoxi.minY(), shipBoxi.minZ(),
				shipBoxi.maxX(), shipBoxi.maxY(), shipBoxi.maxZ()
			);
			for (final Entity entity : this.oldLevel.getEntities(
				((Entity)(null)),
				new AABB(
					shipYardBox.minX - 16 * 4, shipYardBox.minY - 16 * 4, shipYardBox.minZ - 16 * 4,
					shipYardBox.maxX + 16 * 4, shipYardBox.maxY + 16 * 4, shipYardBox.maxZ + 16 * 4
				),
				(entity) -> !this.entityToPos.containsKey(entity)
			)) {
				this.collectEntity(entity, origin, newPos, rotation);
			}
			shipBoxd.union(shipYardBox.transform(ship.getPrevTickTransform().getShipToWorld()));
		}
		double scaledEntityCollectRange = ENTITY_COLLECT_RANGE;
		if (GenesisMod.isMiniScale(ResourceLocation.parse(ship.getChunkClaimDimension().substring(20)))) {
			scaledEntityCollectRange /= 16;
		}

		final AABB inflatedBox = new AABB(
			shipBoxd.minX - scaledEntityCollectRange, shipBoxd.minY - scaledEntityCollectRange, shipBoxd.minZ - scaledEntityCollectRange,
			shipBoxd.maxX + scaledEntityCollectRange, shipBoxd.maxY + scaledEntityCollectRange, shipBoxd.maxZ + scaledEntityCollectRange
		);
		for (final Entity entity : this.oldLevel.getEntities(
			((Entity)(null)),
			inflatedBox,
			(entity) -> !this.entityToPos.containsKey(entity)
		)) {
			this.collectEntity(entity, origin, newPos, rotation);
		}
	}

	private void collectEntity(final Entity entity, final Vector3dc origin, final Vector3dc newPos, final Quaterniondc rotation) {
		final Entity root = entity.getRootVehicle();
		if (this.entityToPos.containsKey(root)) {
			return;
		}
		Vec3 pos = root.position();
		if (!VSGameUtilsKt.isBlockInShipyard(this.oldLevel, pos)) {
			final Vector3d relPos = new Vector3d(pos.x, pos.y, pos.z).sub(origin);
			rotation.transform(relPos);
			relPos.add(newPos);
			pos = new Vec3(relPos.x, relPos.y, relPos.z);
		}
		this.entityToPos.put(root, pos);
	}

	private void teleportEntities() {
		this.entityToPos.keySet().forEach((entity) -> {
			if (entity instanceof ISpecialTeleportLogicEntity specialEntity) {
				specialEntity.genesis$beforeTeleport();
			}
		});
		this.entityToPos.forEach((entity, newPos) -> {
			teleportToWithPassengers(entity, this.newLevel, newPos);
		});
		this.entityToPos.clear();
	}

	private void handleShipTeleport(final long id, final TeleportData data) {
		final String vsDimName = VSGameUtilsKt.getDimensionId(this.newLevel);
		final Vector3dc newPos = data.newPos();
		final Quaterniondc rotation = data.rotation();
		final Vector3dc velocity = data.velocity();
		final Vector3dc omega = data.omega();

		// Get ship scale from VLib dimension settings
		final double shipScale = DimensionSettingsManager.INSTANCE.getSettingsForLevel(vsDimName).getShipScale();

		final LoadedServerShip ship = this.shipWorld.getLoadedShips().getById(id);
		if (ship == null) {
//			final PhysicsEntityServer physEntity = (this.shipWorld).getLoadedShips().getById(id);
//			if (physEntity == null) {
//				LOGGER.warn("[genesis]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity!");
//				return;
//			}
//			LOGGER.info("[genesis]: Teleporting physics entity {} to {} {} (scale: {})", id, vsDimName, newPos, shipScale);
//			final ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos, physEntity.getShipTransform().getShipToWorldRotation(), physEntity.getLinearVelocity(), physEntity.getAngularVelocity(), vsDimName, shipScale);
//			this.shipWorld.teleportPhysicsEntity(physEntity, teleportData);
			return;
		}

		LOGGER.info("[genesis]: Teleporting ship {} ({}) to {} {} (scale: {})", ship.getSlug(), id, vsDimName, newPos, shipScale);
		ship.setStatic(false);
		final ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos, rotation, velocity, omega, vsDimName, shipScale, null);
		this.shipWorld.teleportShip(ship, teleportData);
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform prevTransform, final ShipTransform transform) {
					final LoadedServerShip ship2 = TeleportationHandler.this.shipWorld.getLoadedShips().getById(id);
					if (!prevTransform.getPositionInWorld().equals(transform.getPositionInWorld()) || !prevTransform.getShipToWorldRotation().equals(transform.getShipToWorldRotation())) {
						ship2.setTransformProvider(null);
						return null;
					}
					if (ship2.getVelocity().lengthSquared() == 0 && ship2.getOmega().lengthSquared() == 0) {
						return new NextTransformAndVelocityData(transform, velocity, omega);
					}
					return null;
				}
			});
		}
	}

	private static <T extends Entity> T teleportToWithPassengers(final T entity, final ServerLevel newLevel, final Vec3 newPos) {
		final Vec3 oldPos = entity.position();
		final List<Entity> passengers = new ArrayList<>(entity.getPassengers());
		passengers.forEach((e) -> {
			if (e instanceof ISpecialTeleportLogicEntity specialEntity) {
				specialEntity.genesis$beforeTeleport();
			}
		});
		final T newEntity;
		if (entity instanceof ServerPlayer player) {
			player.teleportTo(newLevel, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
			newEntity = entity;
		} else {
			newEntity = (T) entity.getType().create(newLevel);
			if (newEntity == null) {
				if (entity instanceof ISpecialTeleportLogicEntity specialEntity) {
					specialEntity.genesis$afterTeleport(null);
				}
				return null;
			}
			entity.ejectPassengers();
			newEntity.restoreFrom(entity);
			newEntity.moveTo(newPos.x, newPos.y, newPos.z, newEntity.getYRot(), newEntity.getXRot());
			newEntity.setYHeadRot(entity.getYHeadRot());
			newEntity.setYBodyRot(entity.getVisualRotationYInDegrees());
			newLevel.addDuringTeleport(newEntity);
			entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
		}
		for (final Entity p : passengers) {
			final Entity newPassenger = teleportToWithPassengers(p, newLevel, p.position().subtract(oldPos).add(newPos));
			if (newPassenger != null) {
				newPassenger.startRiding(newEntity, true);
			}
		}
		if (newEntity instanceof ISpecialTeleportLogicEntity specialEntity) {
			specialEntity.genesis$afterTeleport((ISpecialTeleportLogicEntity)(entity));
		}
		return newEntity;
	}

	private ServerShip getShip(final long shipId) {
		final ServerShip ship = this.shipWorld.getLoadedShips().getById(shipId);
		if (ship != null) {
			return ship;
		}
		return this.shipWorld.getAllShips().getById(shipId);
	}

	private PreShipTravelEvent createPreShipTravelEvent(
		final ServerShip ship,
		final ResourceKey<Level> oldLevel,
		final ResourceKey<Level> newLevel,
		final Vector3dc position,
		final Quaterniondc rotation,
		final Vector3d velocity,
		final Vector3d omega
	) {
		return this.isReturning
			? new PreShipTravelEvent.SpaceToPlanet(ship, oldLevel, newLevel, position, rotation, velocity, omega)
			: new PreShipTravelEvent.PlanetToSpace(ship, oldLevel, newLevel, position, rotation, velocity, omega);
	}

	private record TeleportData(Vector3d newPos, Quaterniond rotation, Vector3dc velocity, Vector3dc omega) {}
}
