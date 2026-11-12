package shipwrights.genesis.compat.vs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.VsiCore;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/// Moves Valkyrien Skies ships between space and the dimensions of celestials, as {@link CelestialTravel} moves
/// entities: arriving at a celestial when any part comes within {@value CelestialTravel#ARRIVAL_DISTANCE} blocks of it,
/// and leaving a celestial's dimension when any part rises above the exit height. Ships joined to it go with it (see
/// {@link ShipConnections}), and so do the entities aboard them, all in the same tick.
///
/// They keep their places relative to each other, turned as a whole by the frame of the surface where they cross, which
/// turns their velocity and spin too. They arrive in a celestial's dimension with their top at the entry height, and in
/// space with their nearest part {@value CelestialTravel#DEPARTURE_DISTANCE} blocks out. Static ships, and ships held
/// to the world by a joint, stay where they are.
final class ShipTravel {

    private ShipTravel() {
    }

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        boolean inSpace = GenesisMod.isSpaceDimension(level);
        Optional<Celestial> celestial = GenesisMod.celestial(level);
        if (!inSpace && celestial.isEmpty()) return;

        VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        String dimension = VSGameUtilsKt.getDimensionId(level);
        List<LoadedServerShip> ships = new ArrayList<>();
        for (LoadedServerShip ship : shipWorld.getLoadedShips()) {
            if (dimension.equals(ship.getChunkClaimDimension())) ships.add(ship);
        }
        if (ships.isEmpty()) return;

        Collection<Long> groundBodies = shipWorld.getDimensionToGroundBodyIdImmutable().values();
        List<CelestialTravel.Destination> destinations = inSpace ? CelestialTravel.destinations(level) : List.of();
        Set<Long> seen = new HashSet<>();
        for (LoadedServerShip ship : ships) {
            if (seen.contains(ship.getId())) continue;
            Set<Long> ids = ShipConnections.group(dimension, ship.getId(), groundBodies);
            seen.addAll(ids);
            List<LoadedServerShip> group = loadedGroup(shipWorld, dimension, ids);
            if (group.isEmpty()) continue;

            if (inSpace) {
                tickInSpace(level, destinations, ship, group);
            } else {
                tickOnCelestial(level, celestial.get(), ship, group);
            }
        }
    }

    /// The ships of {@code ids}, if they can travel: all loaded in this dimension, none static, and not held to the
    /// world
    private static List<LoadedServerShip> loadedGroup(VsiServerShipWorld shipWorld, String dimension, Set<Long> ids) {
        if (ids.contains(ShipConnections.ANCHOR)) return List.of();
        List<LoadedServerShip> group = new ArrayList<>();
        for (long id : ids) {
            LoadedServerShip ship = shipWorld.getLoadedShips().getById(id);
            if (ship == null || ship.isStatic() || !dimension.equals(ship.getChunkClaimDimension())) return List.of();
            group.add(ship);
        }
        return group;
    }

    private static void tickInSpace(ServerLevel space, List<CelestialTravel.Destination> destinations,
                                    LoadedServerShip ship, List<LoadedServerShip> group) {
        for (LoadedServerShip member : group) {
            AABBic shipAABB = member.getShipAABB();
            if (shipAABB == null) continue;
            OBB obb = OBB.fromShip(shipAABB, member.getShipToWorld());
            for (CelestialTravel.Destination destination : destinations) {
                if (destination.placement().obb().distanceTo(obb) < CelestialTravel.ARRIVAL_DISTANCE) {
                    arrive(space, destination, ship, group);
                    return;
                }
            }
        }
    }

    private static void arrive(ServerLevel space, CelestialTravel.Destination destination, LoadedServerShip ship,
                               List<LoadedServerShip> group) {
        AABBd bounds = bounds(group);
        Vector3d reference = bounds.center(new Vector3d());
        CelestialTravel.Crossing crossing = CelestialTravel.intoDimension(destination.placement(), reference, 0);

        // The group's top, as it'll be turned in the dimension, at the entry height
        double top = Double.NEGATIVE_INFINITY;
        for (LoadedServerShip member : group) {
            for (Vector3d corner : corners(member.getWorldAABB())) {
                top = Math.max(top, crossing.rotation().transform(corner.sub(reference)).y);
            }
        }
        Vector3d target = new Vector3d(crossing.position().x, CelestialTravel.arrivalHeight() - top,
                crossing.position().z);

        move(space, destination.level(), destination.id(), CelestialTravelEvent.Direction.TO_CELESTIAL, ship, group,
                reference, target, crossing.rotation());
    }

    private static void tickOnCelestial(ServerLevel level, Celestial celestial, LoadedServerShip ship,
                                        List<LoadedServerShip> group) {
        int exitHeight = GenesisCommonConfig.atmosphereExitHeight();
        boolean above = group.stream().anyMatch(member -> member.getWorldAABB().maxY() > exitHeight);
        if (!above) return;
        CelestialTravel.space(level).ifPresent(space -> leave(level, space, celestial, ship, group));
    }

    private static void leave(ServerLevel level, ServerLevel space, Celestial celestial, LoadedServerShip ship,
                              List<LoadedServerShip> group) {
        AABBd bounds = bounds(group);
        Vector3d reference = bounds.center(new Vector3d());
        // Up in the dimension is out from the celestial's surface, so the group's bottom is what's nearest it
        double below = reference.y - bounds.minY;
        CelestialTravel.Crossing crossing = CelestialTravel.intoSpace(CelestialTravel.placement(space, celestial),
                celestial, reference.x, reference.z, CelestialTravel.DEPARTURE_DISTANCE + below);
        Vec3 position = crossing.position();

        move(level, space, level.dimension().location(), CelestialTravelEvent.Direction.TO_SPACE, ship, group,
                reference, new Vector3d(position.x, position.y, position.z), crossing.rotation());
    }

    /// Moves {@code group}, and the entities aboard its ships, from {@code from} to {@code to}, turning them by
    /// {@code rotation} about {@code reference} and putting {@code reference} at {@code target}
    private static void move(ServerLevel from, ServerLevel to, ResourceLocation celestial,
                             CelestialTravelEvent.Direction direction, LoadedServerShip ship,
                             List<LoadedServerShip> group, Vector3dc reference, Vector3dc target,
                             Quaterniondc rotation) {
        Set<Long> ids = new HashSet<>();
        for (LoadedServerShip member : group) ids.add(member.getId());
        if (NeoForge.EVENT_BUS.post(new CelestialShipTravelEvent(ship, Set.copyOf(ids), celestial, direction, from,
                to)).isCanceled()) {
            return;
        }

        // Found before anything moves, while they're still where they were
        List<Entity> inShipyards = new ArrayList<>();
        List<Entity> standingOn = new ArrayList<>();
        for (Entity entity : from.getAllEntities()) {
            if (entity.isPassenger() || !CelestialTravel.canTravel(entity, from, to)) continue;
            Ship managing = VSGameUtilsKt.getShipManagingPos(from, entity.position());
            if (managing != null) {
                if (ids.contains(managing.getId())) inShipyards.add(entity);
            } else if (ids.contains(VsCompat.shipStoodOn(entity))) {
                standingOn.add(entity);
            }
        }

        // Valkyrien Skies moves each ship, and its shipyard, to the new dimension within the call
        VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(from);
        VsiCore core = VSGameUtilsKt.getVsCore();
        String dimension = VSGameUtilsKt.getDimensionId(to);
        for (LoadedServerShip member : group) {
            ShipTransform transform = member.getTransform();
            Vector3d position = rotation.transform(new Vector3d(transform.getPositionInWorld()).sub(reference))
                    .add(target);
            Quaterniond turned = new Quaterniond(rotation).mul(transform.getShipToWorldRotation());
            Vector3d velocity = rotation.transform(new Vector3d(member.getVelocity()));
            Vector3d omega = rotation.transform(new Vector3d(member.getOmega()));
            shipWorld.teleportShip(member, core.newShipTeleportData(position, turned, velocity, omega, dimension,
                    null, null));
        }

        // In their ships' own coordinates, which go with them unchanged
        for (Entity entity : inShipyards) {
            CelestialTravel.carry(entity, from, to, entity.position(), new Quaterniond());
        }
        for (Entity entity : standingOn) {
            Vector3d position = rotation.transform(new Vector3d(entity.getX(), entity.getY(), entity.getZ())
                    .sub(reference)).add(target);
            CelestialTravel.carry(entity, from, to, new Vec3(position.x, position.y, position.z), rotation);
        }
    }

    private static AABBd bounds(List<LoadedServerShip> group) {
        AABBd bounds = new AABBd(group.get(0).getWorldAABB());
        for (LoadedServerShip member : group) bounds.union(member.getWorldAABB());
        return bounds;
    }

    private static List<Vector3d> corners(AABBdc box) {
        List<Vector3d> corners = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            corners.add(new Vector3d(
                    (i & 1) == 0 ? box.minX() : box.maxX(),
                    (i & 2) == 0 ? box.minY() : box.maxY(),
                    (i & 4) == 0 ? box.minZ() : box.maxZ()));
        }
        return corners;
    }
}
