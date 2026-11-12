package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicket;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import g_mungus.zps.gas.api.GridTransfer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBi;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.ChunkMapInvoker;
import shipwrights.genesis.mixin.ServerChunkCacheInvoker;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/// Moves Sable sub-levels between space and the dimensions of celestials, as {@link CelestialTravel} moves entities:
/// arriving at a celestial when any part comes within {@value CelestialTravel#ARRIVAL_DISTANCE} blocks of it, and
/// leaving a celestial's dimension when any part rises above the exit height. Sub-levels connected to it go with it (see
/// {@link SubLevelHelper#getConnectedChain}), and so do the entities aboard them and ZPS's reactors and rooms in them,
/// all in the same tick.
///
/// They keep their places relative to each other, turned as a whole by the frame of the surface where they cross, which
/// turns their velocity and spin too. They arrive in a celestial's dimension with their top at the entry height, and in
/// space with their nearest part {@value CelestialTravel#DEPARTURE_DISTANCE} blocks out.
///
/// Sable can only move a sub-level within its own dimension. So each is rebuilt in the other: a sub-level is made
/// there, and Sable's own assembly moves the blocks into its plot, telling the blocks that listen for it (see
/// {@link dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener}) where they went. The old one is then removed. The
/// new one has the same plot and id, and its blocks the same places in it, so what refers to them, as joined sub-levels
/// do, still finds them: see {@link #allocate}. Where that can't be, what's in the plot moves by the same offset.
///
/// Sable doesn't know which sub-levels are held to the world, as constraints are kept by whatever makes them, so unlike
/// Valkyrien Skies ships, those still travel
final class SubLevelTravel {

    /// Keeps a sub-level loaded after arriving, until the game time it holds
    private static final SubLevelLoadingTicketType<Long> ARRIVAL =
            SubLevelLoadingTicketType.create(GenesisMod.id("arrival"), Codec.LONG);

    /// How long a sub-level is kept loaded after arriving: as long as a nether portal keeps its arrival loaded
    private static final int ARRIVAL_TICKS = 300;

    private SubLevelTravel() {
    }

    /// Before any level loads, as Sable reads the sub-levels' tickets with them, {@link #ARRIVAL}'s among them
    static void init(IEventBus gameBus) {
        gameBus.addListener(SubLevelTravel::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        boolean inSpace = GenesisMod.isSpaceDimension(level);
        Optional<Celestial> celestial = GenesisMod.celestial(level);
        if (!inSpace && celestial.isEmpty()) return;

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        expireArrivals(level, container);
        List<ServerSubLevel> subLevels = new ArrayList<>();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel server && !server.isRemoved()) subLevels.add(server);
        }
        if (subLevels.isEmpty()) return;

        List<CelestialTravel.Destination> destinations = inSpace ? CelestialTravel.destinations(level) : List.of();
        for (List<ServerSubLevel> group : groups(level, subLevels)) {
            if (inSpace) {
                tickInSpace(level, destinations, group);
            } else {
                tickOnCelestial(level, celestial.get(), group);
            }
        }
    }

    /// The sub-levels of {@code level}, gathered into groups that are connected, whichever of them declares the
    /// connection. Groups reaching sub-levels elsewhere, which can't travel with them, are left out
    private static List<List<ServerSubLevel>> groups(ServerLevel level, List<ServerSubLevel> subLevels) {
        Map<UUID, ServerSubLevel> byId = new LinkedHashMap<>();
        for (ServerSubLevel subLevel : subLevels) byId.put(subLevel.getUniqueId(), subLevel);

        Map<UUID, UUID> parents = new HashMap<>();
        Set<UUID> stranded = new HashSet<>();
        for (ServerSubLevel subLevel : subLevels) {
            for (SubLevel connected : SubLevelHelper.getConnectedChain(subLevel)) {
                if (connected.getLevel() != level || connected.isRemoved()
                        || !byId.containsKey(connected.getUniqueId())) {
                    stranded.add(subLevel.getUniqueId());
                    continue;
                }
                union(parents, subLevel.getUniqueId(), connected.getUniqueId());
            }
        }

        Map<UUID, List<ServerSubLevel>> groups = new LinkedHashMap<>();
        for (ServerSubLevel subLevel : subLevels) {
            groups.computeIfAbsent(find(parents, subLevel.getUniqueId()), id -> new ArrayList<>()).add(subLevel);
        }
        List<List<ServerSubLevel>> result = new ArrayList<>();
        for (List<ServerSubLevel> group : groups.values()) {
            if (group.stream().noneMatch(member -> stranded.contains(member.getUniqueId()))) result.add(group);
        }
        return result;
    }

    private static UUID find(Map<UUID, UUID> parents, UUID id) {
        UUID parent = parents.getOrDefault(id, id);
        if (parent.equals(id)) return id;
        UUID root = find(parents, parent);
        parents.put(id, root);
        return root;
    }

    private static void union(Map<UUID, UUID> parents, UUID a, UUID b) {
        UUID rootA = find(parents, a);
        UUID rootB = find(parents, b);
        if (!rootA.equals(rootB)) parents.put(rootA, rootB);
    }

    private static void tickInSpace(ServerLevel space, List<CelestialTravel.Destination> destinations,
                                    List<ServerSubLevel> group) {
        for (ServerSubLevel member : group) {
            OBB obb = obb(member);
            if (obb == null) continue;
            for (CelestialTravel.Destination destination : destinations) {
                if (destination.placement().obb().distanceTo(obb) < CelestialTravel.ARRIVAL_DISTANCE) {
                    arrive(space, destination, member, group);
                    return;
                }
            }
        }
    }

    private static void arrive(ServerLevel space, CelestialTravel.Destination destination, ServerSubLevel subLevel,
                               List<ServerSubLevel> group) {
        AABBd bounds = bounds(group);
        Vector3d reference = bounds.center(new Vector3d());
        CelestialTravel.Crossing crossing = CelestialTravel.intoDimension(destination.placement(), reference, 0);

        // The group's top, as it'll be turned in the dimension, at the entry height
        double top = Double.NEGATIVE_INFINITY;
        for (ServerSubLevel member : group) {
            for (Vector3d corner : corners(member.boundingBox())) {
                top = Math.max(top, crossing.rotation().transform(corner.sub(reference)).y);
            }
        }
        Vector3d target = new Vector3d(crossing.position().x, CelestialTravel.arrivalHeight() - top,
                crossing.position().z);

        move(space, destination.level(), destination.id(), CelestialTravelEvent.Direction.TO_CELESTIAL, subLevel,
                group, reference, target, crossing.rotation());
    }

    private static void tickOnCelestial(ServerLevel level, Celestial celestial, List<ServerSubLevel> group) {
        int exitHeight = GenesisCommonConfig.atmosphereExitHeight();
        for (ServerSubLevel member : group) {
            if (member.boundingBox().maxY() > exitHeight) {
                CelestialTravel.space(level).ifPresent(space -> leave(level, space, celestial, member, group));
                return;
            }
        }
    }

    private static void leave(ServerLevel level, ServerLevel space, Celestial celestial, ServerSubLevel subLevel,
                              List<ServerSubLevel> group) {
        AABBd bounds = bounds(group);
        Vector3d reference = bounds.center(new Vector3d());
        // Up in the dimension is out from the celestial's surface, so the group's bottom is what's nearest it
        double below = reference.y - bounds.minY;
        CelestialTravel.Crossing crossing = CelestialTravel.intoSpace(CelestialTravel.placement(space, celestial),
                celestial, reference.x, reference.z, CelestialTravel.DEPARTURE_DISTANCE + below);
        Vec3 position = crossing.position();

        move(level, space, level.dimension().location(), CelestialTravelEvent.Direction.TO_SPACE, subLevel, group,
                reference, new Vector3d(position.x, position.y, position.z), crossing.rotation());
    }

    /// Moves {@code group}, and the entities aboard its sub-levels, from {@code from} to {@code to}, turning them by
    /// {@code rotation} about {@code reference} and putting {@code reference} at {@code target}
    private static void move(ServerLevel from, ServerLevel to, ResourceLocation celestial,
                             CelestialTravelEvent.Direction direction, ServerSubLevel subLevel,
                             List<ServerSubLevel> group, Vector3dc reference, Vector3dc target,
                             Quaterniondc rotation) {
        ServerSubLevelContainer toContainer = SubLevelContainer.getContainer(to);
        if (toContainer == null) return;

        Set<UUID> ids = new HashSet<>();
        for (ServerSubLevel member : group) ids.add(member.getUniqueId());
        if (NeoForge.EVENT_BUS.post(new CelestialSubLevelTravelEvent(subLevel, Set.copyOf(ids), celestial, direction,
                from, to)).isCanceled()) {
            return;
        }

        // Found before anything moves, while they're still where they were
        Map<Entity, InPlot> inPlots = new LinkedHashMap<>();
        Map<Entity, Vec3> standingOn = new LinkedHashMap<>();
        for (Entity entity : from.getAllEntities()) {
            if (entity.isPassenger() || !CelestialTravel.canTravel(entity, from, to)) continue;
            SubLevelAccess containing = SableCompanion.INSTANCE.getContaining(entity);
            if (containing != null) {
                if (ids.contains(containing.getUniqueId())) {
                    List<Rider> riders = new ArrayList<>();
                    for (Entity rider : entity.getPassengers()) {
                        riders.add(new Rider(rider, arrivingAt(rider, reference, target, rotation),
                                velocityAboard(from, containing, rider)));
                    }
                    inPlots.put(entity, new InPlot(containing.getUniqueId(), entity.position(),
                            CelestialTravel.velocity(entity, from), riders));
                }
                continue;
            }
            SubLevelAccess aboard = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);
            if (aboard != null && ids.contains(aboard.getUniqueId())) {
                standingOn.put(entity, velocityAboard(from, aboard, entity));
            }
        }

        Map<UUID, Rebuilt> rebuilt = new HashMap<>();
        for (ServerSubLevel member : group) {
            rebuilt.put(member.getUniqueId(), rebuild(member, from, to, toContainer, reference, target, rotation));
        }

        // What's in their plots with no player among it goes first, before Sable puts it out of them or destroys it,
        // as it does some seats
        for (Iterator<Map.Entry<Entity, InPlot>> iterator = inPlots.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Entity, InPlot> entry = iterator.next();
            if (hasPlayer(entry.getKey())) continue;
            carryInPlot(entry.getKey(), entry.getValue(), rebuilt, from, to, rotation);
            iterator.remove();
        }

        // Removed before the players aboard leave, while they're still here. Sable tells everyone who was following a
        // sub-level that it's gone, wherever they are by then, by where its plot is; and a client already in the other
        // dimension would take that for whatever's in that plot there, as the sub-level it's arriving on may well be.
        // Now, too, rather than left empty for Sable to remove on its next tick, so nothing in between finds them.
        // What's still in their plots is put out into the world
        ServerSubLevelContainer fromContainer = SubLevelContainer.getContainer(from);
        for (ServerSubLevel member : group) {
            if (!member.isRemoved()) fromContainer.removeSubLevel(member, SubLevelRemovalReason.REMOVED);
        }

        // Then the players' seats, with them
        for (Map.Entry<Entity, InPlot> entry : inPlots.entrySet()) {
            carryInPlot(entry.getKey(), entry.getValue(), rebuilt, from, to, rotation);
        }
        for (Map.Entry<Entity, Vec3> entry : standingOn.entrySet()) {
            Entity entity = entry.getKey();
            CelestialTravel.carry(entity, from, to, arrivingAt(entity, reference, target, rotation), rotation,
                    entry.getValue());
        }
    }

    /// {@code entity} to where it was in its plot, which has moved, but turns with it unchanged, and what was riding
    /// it to where they were in the world, turned with it, seated on it again. Sable keeps what rides something in a
    /// plot out in the world, where it's seen: a player put in the plot instead would be following its chunks as the
    /// world's, and its client, told to forget them as Sable put it back out, disconnects
    private static void carryInPlot(Entity entity, InPlot inPlot, Map<UUID, Rebuilt> rebuilt, ServerLevel from,
                                    ServerLevel to, Quaterniondc rotation) {
        Entity moved = null;
        if (!entity.isRemoved()) {
            entity.ejectPassengers();
            Vec3i offset = rebuilt.get(inPlot.subLevel()).offset();
            moved = CelestialTravel.carry(entity, from, to,
                    inPlot.position().add(offset.getX(), offset.getY(), offset.getZ()), new Quaterniond(),
                    inPlot.velocity());
        }
        // Where Sable has destroyed a seat with its sub-level, as it does Create's, what was riding it goes alone
        for (Rider rider : inPlot.riders()) {
            Entity entity1 = rider.entity();
            if (entity1.isRemoved() || entity1.isPassenger()) continue;
            Entity movedRider = CelestialTravel.carry(entity1, from, to, rider.position(), rotation, rider.velocity());
            if (moved != null && movedRider != null) movedRider.startRiding(moved, true);
        }
    }

    /// Where {@code entity}, out in the world, arrives were the whole group turned by {@code rotation} about
    /// {@code reference} and {@code reference} put at {@code target}
    private static Vec3 arrivingAt(Entity entity, Vector3dc reference, Vector3dc target, Quaterniondc rotation) {
        Vector3d position = rotation.transform(new Vector3d(entity.getX(), entity.getY(), entity.getZ())
                .sub(reference)).add(target);
        return new Vec3(position.x, position.y, position.z);
    }

    private static boolean hasPlayer(Entity root) {
        return root.getSelfAndPassengers().anyMatch(entity -> entity instanceof Player);
    }

    /// How fast {@code entity}, standing on or riding {@code subLevel}, is moving, in blocks a tick. Sable moves what's
    /// aboard by moving it with the sub-level, not by its velocity, which is only how it moves on the sub-level; so add
    /// the sub-level's own where it is. Clients move their players, though, so how far one moved last tick already
    /// includes how the sub-level moved it
    private static Vec3 velocityAboard(ServerLevel level, SubLevelAccess subLevel, Entity entity) {
        Vec3 velocity = CelestialTravel.velocity(entity, level);
        if (CelestialTravel.isMovedByClient(entity)) return velocity;
        // Sable's velocities are in blocks a second
        return velocity.add(SableCompanion.INSTANCE.getVelocity(level, subLevel, entity.position()).scale(1.0 / 20));
    }

    /// Makes a sub-level in {@code to} holding {@code old}'s blocks, placed, turned and moving as {@code old} would be
    /// were the whole group turned by {@code rotation} about {@code reference} and {@code reference} put at
    /// {@code target}. Leaves {@code old} empty, to be removed
    ///
    /// @return the new sub-level, and how far the blocks moved in the plot grid to it
    private static Rebuilt rebuild(ServerSubLevel old, ServerLevel from, ServerLevel to,
                                 ServerSubLevelContainer toContainer, Vector3dc reference, Vector3dc target,
                                 Quaterniondc rotation) {
        LevelPlot plot = old.getPlot();
        BoundingBox3ic bounds = plot.getBoundingBox();
        Pose3d oldPose = new Pose3d(old.logicalPose());

        Vector3d linearVelocity = new Vector3d();
        Vector3d angularVelocity = new Vector3d();
        RigidBodyHandle handle = RigidBodyHandle.of(old);
        if (handle != null) {
            handle.getLinearVelocity(linearVelocity);
            handle.getAngularVelocity(angularVelocity);
        }

        // Made where it's going, so Sable never sees it anywhere else in the new dimension. Where its blocks land in the
        // plot isn't known yet, so neither is the point it turns about there; but that point, wherever it is, is put at
        // the same place
        Quaterniond orientation = new Quaterniond(rotation).mul(oldPose.orientation());
        Pose3d arrivalPose = new Pose3d(oldPose);
        arrivalPose.position().set(rotation.transform(new Vector3d(oldPose.position()).sub(reference)).add(target));
        arrivalPose.orientation().set(orientation);
        ServerSubLevel rebuilt = allocate(old, toContainer, arrivalPose);
        LevelPlot newPlot = rebuilt.getPlot();
        BlockPos anchor = plot.getCenterBlock();
        // In the same plot, its blocks stay where they were in it, so what refers to them by where they are still
        // finds them, unless the new dimension is too short for them
        boolean samePlot = newPlot.plotPos.equals(plot.plotPos);
        boolean fits = bounds == BoundingBox3i.EMPTY
                || (bounds.minY() >= to.getMinBuildHeight() && bounds.maxY() < to.getMaxBuildHeight());
        BlockPos newAnchor = samePlot && fits ? anchor : newPlot.getCenterBlock();
        Vec3i offset = newAnchor.subtract(anchor);

        if (bounds != BoundingBox3i.EMPTY) {
            List<BlockPos> blocks = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                if (!from.getBlockState(pos).isAir()) blocks.add(pos.immutable());
            }

            // Sable's assembly only makes the chunks the blocks land in when it's in the same dimension
            for (int chunkX = (bounds.minX() + offset.getX()) >> 4; chunkX <= (bounds.maxX() + offset.getX()) >> 4; chunkX++) {
                for (int chunkZ = (bounds.minZ() + offset.getZ()) >> 4; chunkZ <= (bounds.maxZ() + offset.getZ()) >> 4; chunkZ++) {
                    ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                    if (newPlot.getChunkHolder(newPlot.toLocal(chunk)) == null) newPlot.newEmptyChunk(chunk);
                }
            }
            // Sable's new chunks are only found in the level once its chunk map next shows the chunks it's been given,
            // and its chunk cache forgets what it last found there, as both do each tick. Until then, finding a block
            // entity there finds none, so Sable's assembly wouldn't load the moved ones' data into them, nor keep track
            // of those that act on the sub-level, as a swivel bearing's plate does to drive its joint. Do both now
            ((ChunkMapInvoker) to.getChunkSource().chunkMap).genesis$promoteChunkMap();
            ((ServerChunkCacheInvoker) to.getChunkSource()).genesis$clearCache();

            if (!blocks.isEmpty()) {
                // ZPS keeps its reactors and rooms per dimension, by position, so they're taken out before the blocks
                // go, while their chambers and ports can still be read, and nothing in the plot can fail as it's
                // emptied. Then they're put where the blocks went
                GridTransfer.Cargo cargo = GridTransfer.detach(from, GridTransfer.Region.chunks(plot::contains));
                moveBlocks(from, to, anchor, newAnchor, blocks);
                cargo.attach(to, new BlockPos(offset));
            }
        }

        rebuilt.setName(old.getName());
        CompoundTag userData = old.getUserDataTag();
        if (userData != null) rebuilt.setUserDataTag(userData.copy());

        // The point it turns about is its centre of mass, which Sable has found in the new plot by now
        Vector3dc centreOfMass = rebuilt.getMassTracker().getCenterOfMass();
        Vector3d rotationPoint = centreOfMass != null
                ? new Vector3d(centreOfMass)
                : new Vector3d(oldPose.rotationPoint()).add(offset.getX(), offset.getY(), offset.getZ());
        Vector3d oldWorld = oldPose.transformPosition(new Vector3d(rotationPoint)
                .sub(offset.getX(), offset.getY(), offset.getZ()));
        Vector3d position = rotation.transform(oldWorld.sub(reference)).add(target);

        Pose3d pose = rebuilt.logicalPose();
        pose.rotationPoint().set(rotationPoint);
        pose.position().set(position);
        pose.orientation().set(orientation);

        if (rebuilt.isRemoved()) return new Rebuilt(rebuilt, offset);

        PhysicsPipeline pipeline = toContainer.physicsSystem().getPipeline();
        pipeline.teleport(rebuilt, position, orientation);
        rebuilt.updateLastPose();
        rebuilt.updateBoundingBox();
        rebuilt.forceUpdateGlobalBounds();
        pipeline.addLinearAndAngularVelocity(rebuilt, rotation.transform(linearVelocity),
                rotation.transform(angularVelocity));

        keepLoaded(toContainer, rebuilt, to.getGameTime());
        return new Rebuilt(rebuilt, offset);
    }

    /// A sub-level for {@code old} in {@code container}: in the same plot, with the same id, so whatever refers to it by
    /// them, as joined sub-levels do, still finds it. Plots are given out where no dimension has one (see
    /// {@link shipwrights.genesis.compat.sable.mixin.SubLevelContainerMixin}), so that one is free there, unless it
    /// was given out before Genesis did so. Then it's a new sub-level, elsewhere
    private static ServerSubLevel allocate(ServerSubLevel old, ServerSubLevelContainer container, Pose3d pose) {
        ChunkPos plotPos = old.getPlot().plotPos;
        int x = plotPos.x - container.getOrigin().x;
        int z = plotPos.z - container.getOrigin().y;
        int side = 1 << container.getLogSideLength();
        boolean free = x >= 0 && x < side && z >= 0 && z < side
                && !container.getOccupancy().get(container.getIndex(x, z))
                && container.getSubLevel(x, z) == null
                && container.getSubLevel(old.getUniqueId()) == null;
        return (ServerSubLevel) (free
                ? container.allocateSubLevel(old.getUniqueId(), x, z, pose)
                : container.allocateNewSubLevel(pose));
    }

    /// Keeps {@code subLevel} loaded for a while after arriving, as a nether portal keeps its arrival loaded, so one
    /// arriving alone carries on moving: Sable otherwise puts away any sub-level in a chunk that isn't loaded, and the
    /// chunks it arrives in aren't yet
    private static void keepLoaded(ServerSubLevelContainer container, ServerSubLevel subLevel, long gameTime) {
        container.addForceLoadTicket(subLevel, ARRIVAL, gameTime + ARRIVAL_TICKS);
    }

    /// Lets go of the sub-levels in {@code level} whose time to be kept loaded after arriving is up
    private static void expireArrivals(ServerLevel level, ServerSubLevelContainer container) {
        long gameTime = level.getGameTime();
        List<Map.Entry<ServerSubLevel, Long>> expired = new ArrayList<>();
        for (Map.Entry<ServerSubLevel, Set<SubLevelLoadingTicket<?>>> entry : container.collectForceLoadTickets().entrySet()) {
            for (SubLevelLoadingTicket<?> ticket : entry.getValue()) {
                if (ticket.type() == ARRIVAL && ticket.key() instanceof Long expiry && expiry <= gameTime) {
                    expired.add(Map.entry(entry.getKey(), expiry));
                }
            }
        }
        for (Map.Entry<ServerSubLevel, Long> entry : expired) {
            container.removeForceLoadTicket(entry.getKey(), ARRIVAL, entry.getValue());
        }
    }

    /// Moves {@code blocks} from {@code from} to {@code to} by Sable's assembly, putting {@code anchor} at
    /// {@code newAnchor}
    private static void moveBlocks(ServerLevel from, ServerLevel to, BlockPos anchor, BlockPos newAnchor,
                                   List<BlockPos> blocks) {
        try {
            SubLevelAssemblyHelper.moveBlocks(from, new SubLevelAssemblyHelper.AssemblyTransform(anchor, newAnchor, 0,
                    Rotation.NONE, to), blocks);
        } catch (UnsupportedOperationException e) {
            // Sable's last step, once everything has moved, tells clients the old blocks have gone, but it does so in
            // the level they went to rather than the one they left. Sable only moves blocks within one level, where
            // that's the same. In another, the old blocks' plot isn't there, so Sable refuses. Tell them in the right
            // level instead. Anything else Sable couldn't move it logs and carries on past, so can't reach here
            for (BlockPos block : blocks) {
                from.sendBlockUpdated(block, Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /// Where {@code subLevel}'s blocks are, turned with it
    static OBB obb(ServerSubLevel subLevel) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == BoundingBox3i.EMPTY) return null;
        // The plot's bounds include the blocks at their maxima
        AABBi blocks = new AABBi(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
        return OBB.fromShip(blocks, subLevel.logicalPose().bakeIntoMatrix(new Matrix4d()));
    }

    private record Rebuilt(ServerSubLevel subLevel, Vec3i offset) {
    }

    /// Where an entity was in a sub-level's plot, by its id, how fast it was moving there, in blocks a tick, and what
    /// was riding it
    private record InPlot(UUID subLevel, Vec3 position, Vec3 velocity, List<Rider> riders) {
    }

    /// Something riding what's in a plot, where it arrives, and how fast it was moving, in blocks a tick
    private record Rider(Entity entity, Vec3 position, Vec3 velocity) {
    }

    private static AABBd bounds(List<ServerSubLevel> group) {
        AABBd bounds = null;
        for (ServerSubLevel member : group) {
            BoundingBox3dc box = member.boundingBox();
            AABBd aabb = new AABBd(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            bounds = bounds == null ? aabb : bounds.union(aabb);
        }
        return bounds;
    }

    private static List<Vector3d> corners(BoundingBox3dc box) {
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
