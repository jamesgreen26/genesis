package shipwrights.genesis.compat.sable;

import g_mungus.zps.block.ModBlocks;
import g_mungus.zps.entity.ModEntities;
import g_mungus.zps.entity.OctoMountingEntity;
import g_mungus.zps.reactor.Reactor;
import g_mungus.zps.reactor.ReactorManager;
import g_mungus.zps.room.Room;
import g_mungus.zps.room.RoomManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.animal.Chicken;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/// The tests of {@link SubLevelTravelGameTests}, apart so they're only loaded with Sable
final class SubLevelTravelTestCases {

    private static final String TEST_PREFIX = "genesis_test_";

    /// The names of the sub-levels of the tests running
    private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

    /// Corners of the hollow cubes the ZPS tests make sub-levels of, in the test's structure
    private static final BlockPos SHELL_MIN = new BlockPos(0, 1, 0);
    private static final BlockPos SHELL_MAX = new BlockPos(4, 5, 4);
    private static final ResourceLocation FLUX = ResourceLocation.fromNamespaceAndPath("zps", "flux");

    static void subLevelLeavesForSpaceWithWhatsAboard(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);
        ServerSubLevel subLevel = assemble(overworld, helper.absolutePos(new BlockPos(1, 1, 1)), name, true);
        // What another sub-level joined to it would know it and its blocks by
        UUID id = subLevel.getUniqueId();
        ChestBlockEntity chestBefore = chest(overworld, subLevel);
        helper.assertTrue(chestBefore != null, "no chest in the sub-level");
        BlockPos chestPos = chestBefore.getBlockPos();

        // Just below the exit height, falling, with an armor stand landing on it beside the chest
        Vector3dc start = subLevel.logicalPose().position();
        Vector3d below = new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() - 8, start.z());
        moveSubLevel(overworld, subLevel, below, new Vector3d());
        ArmorStand stand = EntityType.ARMOR_STAND.create(overworld);
        helper.assertTrue(stand != null, "couldn't create an armor stand");
        stand.moveTo(below.x + 1, below.y + 1, below.z + 1);
        overworld.addFreshEntity(stand);
        UUID standId = stand.getUUID();

        // Where they'll arrive in space kept loaded, as a player aboard would keep it: entities only move in loaded chunks,
        // and in space nothing pulls them back down onto the sub-level, which goes on without them
        Celestial celestial = overworldCelestial(space);
        CelestialTravel.Crossing crossing = CelestialTravel.intoSpace(CelestialTravel.placement(space, celestial),
                celestial, below.x, below.z, CelestialTravel.DEPARTURE_DISTANCE);
        forceLoad(helper, space, BlockPos.containing(crossing.position()), 3);

        // Once it's standing on the sub-level, send them up past the exit height
        helper.runAfterDelay(20, () -> {
            SubLevelAccess standingOn = SableCompanion.INSTANCE.getTrackingSubLevel(stand);
            helper.assertTrue(standingOn != null && standingOn.getUniqueId().equals(subLevel.getUniqueId()),
                    "armor stand didn't land on the sub-level: it's at " + stand.position());
            PhysicsPipeline pipeline = SubLevelContainer.getContainer(overworld).physicsSystem().getPipeline();
            pipeline.resetVelocity(subLevel);
            pipeline.addLinearAndAngularVelocity(subLevel, new Vector3d(0, 20, 0), new Vector3d());
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in the overworld");
            ServerSubLevel found = byName(space, name);
            helper.assertTrue(found != null, "no sub-level arrived in space");

            helper.assertTrue(found.getUniqueId().equals(id), "the sub-level arrived with a new id");
            ChestBlockEntity chest = chest(space, found);
            helper.assertTrue(chest != null, "the chest didn't go with the sub-level");
            helper.assertTrue(chest.getBlockPos().equals(chestPos),
                    "the chest moved in the plot, to " + chest.getBlockPos() + " from " + chestPos);
            helper.assertTrue(chest.getItem(0).is(Items.DIAMOND), "the chest's contents didn't go with it");

            Entity arrived = space.getEntity(standId);
            helper.assertTrue(arrived != null, "armor stand on the sub-level didn't go with it");
            helper.assertTrue(found.boundingBox().toMojang().inflate(2).contains(arrived.position()),
                    "armor stand isn't on the sub-level: it's at " + arrived.position() + ", the sub-level at "
                            + found.logicalPose().position());

            OBB cube = cube(space);
            OBB obb = SubLevelTravel.obb(found);
            helper.assertTrue(obb != null, "sub-level arrived without blocks");
            double distance = cube.distanceTo(obb);
            helper.assertTrue(distance > CelestialTravel.DEPARTURE_DISTANCE - 1
                            && distance < CelestialTravel.DEPARTURE_DISTANCE + 3,
                    "sub-level arrived " + distance + " blocks from the overworld");

            // Up, which it was moving, is out from the overworld in space
            Vector3d centre = new Vector3d(found.logicalPose().position());
            Vector3d outward = new Vector3d(centre).sub(cube.closestPointTo(centre)).normalize();
            Vector3d velocity = velocity(found);
            helper.assertTrue(velocity.length() > 0.5 && velocity.normalize().dot(outward) > 0.9,
                    "sub-level isn't moving away from the overworld: " + velocity(found));

            arrived.discard();
        });
    }

    /// As in front of a ZPS controller, whose seat Sable keeps in the sub-level's plot while what rides it stays out in
    /// the world
    static void seatedRiderLeavesForSpace(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);
        BlockPos centre = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos controllerPos = centre.above();
        overworld.setBlock(controllerPos, ModBlocks.OCTO_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
        ServerSubLevel subLevel = assemble(overworld, centre, name, false, List.of(controllerPos));

        // The seat in front of the controller, where it faces, in the plot
        BlockPos inPlot = find(overworld, subLevel, ModBlocks.OCTO_CONTROLLER.get());
        helper.assertTrue(inPlot != null, "the controller isn't in the sub-level");
        BlockPos seatPos = inPlot.relative(Direction.NORTH);
        OctoMountingEntity seat = ModEntities.OCTO_MOUNTING.get().create(overworld);
        helper.assertTrue(seat != null, "couldn't create a seat");
        seat.moveTo(seatPos.getX() + 0.5, seatPos.getY(), seatPos.getZ() + 0.5);
        overworld.addFreshEntity(seat);
        Chicken chicken = EntityType.CHICKEN.create(overworld);
        helper.assertTrue(chicken != null, "couldn't create a chicken");
        chicken.setNoAi(true);
        chicken.moveTo(Vec3.atCenterOf(seatPos));
        overworld.addFreshEntity(chicken);
        chicken.startRiding(seat, true);
        UUID chickenId = chicken.getUUID();

        Vector3dc start = subLevel.logicalPose().position();
        Vector3d below = new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() - 8, start.z());
        moveSubLevel(overworld, subLevel, below, new Vector3d());
        Celestial celestial = overworldCelestial(space);
        CelestialTravel.Crossing crossing = CelestialTravel.intoSpace(CelestialTravel.placement(space, celestial),
                celestial, below.x, below.z, CelestialTravel.DEPARTURE_DISTANCE);
        forceLoad(helper, space, BlockPos.containing(crossing.position()), 3);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(chicken.getVehicle() == seat && !seat.isRemoved(), "chicken isn't seated before leaving");
            PhysicsPipeline pipeline = SubLevelContainer.getContainer(overworld).physicsSystem().getPipeline();
            pipeline.resetVelocity(subLevel);
            pipeline.addLinearAndAngularVelocity(subLevel, new Vector3d(0, 20, 0), new Vector3d());
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in the overworld");
            ServerSubLevel found = byName(space, name);
            helper.assertTrue(found != null, "no sub-level arrived in space");
            Entity arrived = space.getEntity(chickenId);
            helper.assertTrue(arrived != null, "seated chicken didn't go with the sub-level");
            helper.assertTrue(arrived.getVehicle() instanceof OctoMountingEntity,
                    "chicken isn't seated: it's riding " + arrived.getVehicle());
            SubLevelAccess seatIn = SableCompanion.INSTANCE.getContaining(arrived.getVehicle());
            helper.assertTrue(seatIn != null && seatIn.getUniqueId().equals(found.getUniqueId()),
                    "the seat isn't in the sub-level's plot, at " + arrived.getVehicle().position());
            // What rides something in a plot is kept out in the world, beside the sub-level
            helper.assertTrue(!SableCompanion.INSTANCE.isInPlotGrid(arrived),
                    "chicken is in the plot, at " + arrived.position());
            helper.assertTrue(found.boundingBox().toMojang().inflate(3).contains(arrived.position()),
                    "chicken isn't by the sub-level: it's at " + arrived.position() + ", the sub-level at "
                            + found.logicalPose().position());
        });
    }

    private static BlockPos find(ServerLevel level, ServerSubLevel subLevel, Block block) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (level.getBlockState(pos).is(block)) return pos.immutable();
        }
        return null;
    }

    /// Sable removes sub-levels beyond heights in its config, as vanilla removes entities in the void, but not in space,
    /// which has neither, while still doing so elsewhere
    static void noHeightLimitsInSpace(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);

        // Far from every celestial, so nothing arrives anywhere
        BlockPos centre = new BlockPos(3000, 64, 3000);
        forceLoad(helper, space, centre, 1);
        String below = testName(helper);
        String above = testName(helper);
        ServerSubLevel low = assemble(space, centre, below, false);
        moveSubLevel(space, low, new Vector3d(3000.5, -20_000, 3000.5), new Vector3d());
        ServerSubLevel high = assemble(space, centre.offset(5, 0, 0), above, false);
        moveSubLevel(space, high, new Vector3d(3005.5, 200_000, 3000.5), new Vector3d());

        String inOverworld = testName(helper);
        ServerSubLevel sunk = assemble(overworld, helper.absolutePos(new BlockPos(1, 1, 1)), inOverworld, false);
        Vector3dc start = sunk.logicalPose().position();
        moveSubLevel(overworld, sunk, new Vector3d(start.x(), -20_000, start.z()), new Vector3d());

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(byName(space, below) != null, "the sub-level far below in space was removed");
            helper.assertTrue(byName(space, above) != null, "the sub-level far above in space was removed");
            helper.assertTrue(sunk.isRemoved(), "the sub-level far below in the overworld wasn't removed");
            helper.succeed();
        });
    }

    /// Each new sub-level's plot is free in every dimension, so that one travelling keeps it, and its id. A plot is
    /// taken in space that the overworld would otherwise give the next sub-level there
    static void plotsAreUniqueAcrossDimensions(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        ServerSubLevelContainer overworldPlots = SubLevelContainer.getContainer(overworld);
        ServerSubLevelContainer spacePlots = SubLevelContainer.getContainer(space);

        int side = 1 << overworldPlots.getLogSideLength();
        int nextX = -1;
        int nextZ = -1;
        search:
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                int index = overworldPlots.getIndex(x, z);
                if (!overworldPlots.getOccupancy().get(index) && !spacePlots.getOccupancy().get(index)) {
                    nextX = x;
                    nextZ = z;
                    break search;
                }
            }
        }
        helper.assertTrue(nextX >= 0, "no plot free in both dimensions");

        // Taken in space, for as long as it takes the overworld to give out its next plot
        ServerSubLevel taken = (ServerSubLevel) spacePlots.allocateSubLevel(UUID.randomUUID(), nextX, nextZ,
                new dev.ryanhcode.sable.companion.math.Pose3d());
        ServerSubLevel next;
        try {
            next = assemble(overworld, helper.absolutePos(new BlockPos(1, 1, 1)), testName(helper), false);
        } finally {
            spacePlots.removeSubLevel(taken, SubLevelRemovalReason.REMOVED);
        }
        ChunkPos plot = next.getPlot().plotPos;
        helper.assertTrue(plot.x - overworldPlots.getOrigin().x != nextX || plot.z - overworldPlots.getOrigin().y != nextZ,
                "the overworld gave out plot " + nextX + ", " + nextZ + ", which space has");
        helper.succeed();
    }

    /// Sable runs physics for the block entities in a sub-level's plot that ask it to, as ZPS's thrusters do, and the
    /// swivel bearings of Create Aeronautics: they still do so once it's travelled
    static void blockActorsWorkAfterTravel(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);
        BlockPos centre = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos thrusterPos = centre.above();
        overworld.setBlock(thrusterPos, ModBlocks.THRUSTER.get().defaultBlockState(), 3);
        ServerSubLevel subLevel = assemble(overworld, centre, name, false, List.of(thrusterPos));

        BlockPos inPlot = find(overworld, subLevel, ModBlocks.THRUSTER.get());
        helper.assertTrue(inPlot != null, "the thruster isn't in the sub-level");
        helper.assertTrue(isActor(subLevel, overworld.getBlockEntity(inPlot)),
                "the thruster isn't one of the sub-level's actors before travelling");

        Vector3dc start = subLevel.logicalPose().position();
        moveSubLevel(overworld, subLevel, new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() + 10,
                start.z()), new Vector3d(0, 5, 0));

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in the overworld");
            ServerSubLevel found = byName(space, name);
            helper.assertTrue(found != null, "no sub-level arrived in space");
            BlockPos arrived = find(space, found, ModBlocks.THRUSTER.get());
            helper.assertTrue(arrived != null, "the thruster didn't go with the sub-level");
            helper.assertTrue(isActor(found, space.getBlockEntity(arrived)),
                    "the thruster isn't one of the sub-level's actors after travelling");
        });
    }

    private static boolean isActor(ServerSubLevel subLevel, BlockEntity blockEntity) {
        for (BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            if (actor == blockEntity) return true;
        }
        return false;
    }

    /// ZPS keeps the reactors of a level apart from its blocks: one in a sub-level's plot goes with it
    static void reactorTravelsWithSubLevel(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);
        // Plated over once it's a sub-level, as one is built aboard: Sable's assembly moves blocks without ZPS knowing,
        // so a reactor built before wouldn't come along
        ServerSubLevel subLevel = assembleShell(helper, Blocks.STONE.defaultBlockState(), name);
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (overworld.getBlockState(pos).is(Blocks.STONE)) {
                overworld.setBlock(pos, ModBlocks.REINFORCED_PLATING.get().defaultBlockState(), 3);
            }
        }
        UUID id = subLevel.getUniqueId();
        BlockPos inside = cavity(subLevel);
        helper.assertTrue(ReactorManager.get(overworld).reactorForInterior(inside) != null,
                "the plating shell made no reactor in the sub-level");
        lift(overworld, subLevel);

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in the overworld");
            ServerSubLevel found = byName(space, name);
            helper.assertTrue(found != null, "no sub-level arrived in space");
            helper.assertTrue(found.getUniqueId().equals(id), "the sub-level arrived with a new id");
            helper.assertTrue(ReactorManager.get(overworld).reactorForInterior(inside) == null,
                    "the reactor was left behind in the overworld");
            ReactorManager reactors = ReactorManager.get(space);
            Reactor arrived = reactors.reactorForInterior(inside);
            helper.assertTrue(arrived != null, "the reactor didn't arrive in space");
            helper.assertTrue(reactors.reading(space, arrived) != null, "the reactor's chamber isn't simulated in space");
        });
    }

    /// Likewise ZPS's rooms, and the gas in them
    static void roomTravelsWithSubLevel(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);
        ServerSubLevel subLevel = assembleShell(helper, Blocks.STONE.defaultBlockState(), name);
        BlockPos inside = cavity(subLevel);
        addFlux(overworld, inside, 5.0);
        Room sealed = RoomManager.get(overworld).roomAt(inside);
        helper.assertTrue(sealed != null, "gas into the shell made no room");
        double flux = sealed.massKg().getOrDefault(FLUX, 0.0);
        helper.assertTrue(flux >= 5.0, "the room holds " + flux + " kg of flux");
        lift(overworld, subLevel);

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in the overworld");
            ServerSubLevel found = byName(space, name);
            helper.assertTrue(found != null, "no sub-level arrived in space");
            helper.assertTrue(RoomManager.get(overworld).knownRoomAt(inside) == null,
                    "the room was left behind in the overworld");
            Room arrived = RoomManager.get(space).roomAt(inside);
            helper.assertTrue(arrived != null, "the room didn't arrive in space");
            // It leaks toward space from the moment it's there, but slowly
            double now = arrived.massKg().getOrDefault(FLUX, 0.0);
            helper.assertTrue(now > flux * 0.9, "the room arrived with " + now + " kg of flux, of " + flux);
        });
    }

    /// A sub-level of a hollow 5×5×5 cube of {@code wall}, in the test's structure, named {@code name}
    private static ServerSubLevel assembleShell(GameTestHelper helper, BlockState wall, String name) {
        ServerLevel level = helper.getLevel();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos relative : BlockPos.betweenClosed(SHELL_MIN, SHELL_MAX)) {
            boolean edge = relative.getX() == SHELL_MIN.getX() || relative.getX() == SHELL_MAX.getX()
                    || relative.getY() == SHELL_MIN.getY() || relative.getY() == SHELL_MAX.getY()
                    || relative.getZ() == SHELL_MIN.getZ() || relative.getZ() == SHELL_MAX.getZ();
            BlockPos block = helper.absolutePos(relative);
            level.setBlock(block, edge ? wall : Blocks.AIR.defaultBlockState(), 3);
            if (edge) blocks.add(block.immutable());
        }
        BlockPos min = helper.absolutePos(SHELL_MIN);
        BlockPos max = helper.absolutePos(SHELL_MAX);
        BoundingBox3ic bounds = new BoundingBox3i(
                new BlockPos(Math.min(min.getX(), max.getX()), min.getY(), Math.min(min.getZ(), max.getZ())),
                new BlockPos(Math.max(min.getX(), max.getX()), max.getY(), Math.max(min.getZ(), max.getZ())));
        BlockPos centre = new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, centre, blocks, bounds);
        subLevel.setName(name);
        return subLevel;
    }

    /// The middle of a hollow cube's cavity, in its sub-level's plot, which travelling keeps
    private static BlockPos cavity(ServerSubLevel subLevel) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        return new BlockPos((bounds.minX() + bounds.maxX()) / 2, (bounds.minY() + bounds.maxY()) / 2,
                (bounds.minZ() + bounds.maxZ()) / 2);
    }

    /// Puts {@code subLevel} above the exit height, rising, for it to leave for space
    private static void lift(ServerLevel overworld, ServerSubLevel subLevel) {
        Vector3dc start = subLevel.logicalPose().position();
        moveSubLevel(overworld, subLevel, new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() + 10,
                start.z()), new Vector3d(0, 5, 0));
    }

    /// Puts {@code kg} of ZPS's flux into the air at {@code pos}. By reflection, as the gas's type is Kelvin's, which
    /// Genesis doesn't compile against
    private static void addFlux(ServerLevel level, BlockPos pos, double kg) {
        try {
            Object flux = Class.forName("g_mungus.zps.gas.ModGases").getField("FLUX").get(null);
            RoomManager.class.getMethod("addGas", ServerLevel.class, BlockPos.class, Map.class, double.class)
                    .invoke(RoomManager.get(level), level, pos, Map.of(flux, kg), 300.0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("couldn't put flux into the room", e);
        }
    }

    static void subLevelArrivesFromSpace(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        String name = testName(helper);

        // Just off the overworld's north face, to one side of the middle, away from what the other tests send there, moving towards it
        Celestial celestial = overworldCelestial(space);
        CelestialTravel.Placement placement = CelestialTravel.placement(space, celestial);
        Quaterniondc rotation = placement.rotation();
        Vector3d start = rotation.transform(new Vector3d(-25, 0, -(celestial.size() / 2 + 4))).add(placement.position());
        BlockPos centre = BlockPos.containing(start.x, start.y, start.z);
        ChunkPos chunk = new ChunkPos(centre);
        space.setChunkForced(chunk.x, chunk.z, true);
        ServerSubLevel subLevel = assemble(space, centre, name, false);
        moveSubLevel(space, subLevel, start, rotation.transform(new Vector3d(0, 0, 5)));

        helper.succeedWhen(() -> {
            helper.assertTrue(subLevel.isRemoved(), "sub-level is still in space");
            ServerSubLevel found = byName(overworld, name);
            helper.assertTrue(found != null, "no sub-level arrived in the overworld");
            double top = found.boundingBox().maxY();
            helper.assertTrue(top < GenesisCommonConfig.atmosphereExitHeight()
                            && top > CelestialTravel.arrivalHeight() - 20,
                    "sub-level's top arrived at " + top);
            Vector3dc position = found.logicalPose().position();
            helper.assertTrue(Math.abs(position.x()) < GenesisCommonConfig.worldSize() / 8.0
                            && Math.abs(position.z()) < GenesisCommonConfig.worldSize() / 8.0,
                    "sub-level arrived at " + position + ", off the north face");
            helper.assertTrue(velocity(found).y() < -1, "sub-level isn't moving down: " + velocity(found));

            space.setChunkForced(chunk.x, chunk.z, false);
        });
    }

    /// A sub-level of a 3×3 stone platform centred on {@code centre}, with a chest of a diamond in its middle if
    /// {@code withChest}, named {@code name} to find it again once it's been rebuilt
    static ServerSubLevel assemble(ServerLevel level, BlockPos centre, String name, boolean withChest) {
        return assemble(level, centre, name, withChest, List.of());
    }

    /// As {@link #assemble(ServerLevel, BlockPos, String, boolean)}, with {@code extra}, already placed, too
    private static ServerSubLevel assemble(ServerLevel level, BlockPos centre, String name, boolean withChest,
                                           List<BlockPos> extra) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos block = centre.offset(dx, 0, dz);
                level.setBlock(block, Blocks.STONE.defaultBlockState(), 3);
                blocks.add(block);
            }
        }
        if (withChest) {
            BlockPos chestPos = centre.above();
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.setItem(0, new ItemStack(Items.DIAMOND));
            }
            blocks.add(chestPos);
        }
        blocks.addAll(extra);
        BoundingBox3ic bounds = new BoundingBox3i(centre.offset(-1, 0, -1), centre.offset(1, 1, 1));
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, centre, blocks, bounds);
        subLevel.setName(name);
        return subLevel;
    }

    /// Puts {@code subLevel} at {@code position}, moving at {@code velocity} blocks a second
    static void moveSubLevel(ServerLevel level, ServerSubLevel subLevel, Vector3dc position,
                                     Vector3dc velocity) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        pipeline.teleport(subLevel, position, subLevel.logicalPose().orientation());
        subLevel.logicalPose().position().set(position);
        subLevel.updateLastPose();
        pipeline.resetVelocity(subLevel);
        pipeline.addLinearAndAngularVelocity(subLevel, velocity, new Vector3d());
    }

    private static ServerSubLevel byName(ServerLevel level, String name) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return null;
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel server && !server.isRemoved() && name.equals(server.getName())) {
                return server;
            }
        }
        return null;
    }

    private static ChestBlockEntity chest(ServerLevel level, ServerSubLevel subLevel) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity chest) return chest;
        }
        return null;
    }

    static Vector3d velocity(ServerSubLevel subLevel) {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        return handle == null ? new Vector3d() : handle.getLinearVelocity(new Vector3d());
    }

    /// A name for the sub-levels {@code helper}'s test makes, which are removed when it ends, passed or failed.
    /// Meanwhile, any others a test left behind are removed when found: the game test world is kept, and since the
    /// tests are all far beyond the world size, whatever they send to space arrives at much the same place there,
    /// where those left behind would be in the way
    static String testName(GameTestHelper helper) {
        String name = TEST_PREFIX + UUID.randomUUID();
        RUNNING.add(name);
        MinecraftServer server = helper.getLevel().getServer();
        helper.onEachTick(() -> removeLeftBehind(server));
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo test, GameTestRunner runner) {
                ended();
            }

            @Override
            public void testFailed(GameTestInfo test, GameTestRunner runner) {
                ended();
            }

            @Override
            public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
            }

            private void ended() {
                RUNNING.remove(name);
                removeAll(server, subLevel -> name.equals(subLevel.getName()));
            }
        });
        return name;
    }

    /// Keeps the chunks within {@code radius} of {@code centre} in {@code level} loaded until {@code helper}'s test ends
    static void forceLoad(GameTestHelper helper, ServerLevel level, BlockPos centre, int radius) {
        ChunkPos middle = new ChunkPos(centre);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos chunk = new ChunkPos(middle.x + x, middle.z + z);
                if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk);
            }
        }
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo test, GameTestRunner runner) {
                release();
            }

            @Override
            public void testFailed(GameTestInfo test, GameTestRunner runner) {
                release();
            }

            @Override
            public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
            }

            private void release() {
                for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
            }
        });
    }

    private static void removeLeftBehind(MinecraftServer server) {
        removeAll(server, subLevel -> subLevel.getName() != null && subLevel.getName().startsWith(TEST_PREFIX)
                && !RUNNING.contains(subLevel.getName()));
    }

    private static void removeAll(MinecraftServer server, Predicate<ServerSubLevel> which) {
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) continue;
            for (SubLevel subLevel : List.copyOf(container.getAllSubLevels())) {
                if (subLevel instanceof ServerSubLevel subLevelOnServer && !subLevelOnServer.isRemoved() && which.test(subLevelOnServer)) {
                    container.removeSubLevel(subLevelOnServer, SubLevelRemovalReason.REMOVED);
                }
            }
        }
    }

    static ServerLevel space(GameTestHelper helper) {
        Optional<ServerLevel> space = CelestialTravel.space(helper.getLevel());
        helper.assertTrue(space.isPresent(), "no space dimension");
        return space.get();
    }

    private static Celestial overworldCelestial(ServerLevel space) {
        return space.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY).getOptional(Level.OVERWORLD.location())
                .orElseThrow();
    }

    private static OBB cube(ServerLevel space) {
        return CelestialTravel.placement(space, overworldCelestial(space)).obb();
    }
}
