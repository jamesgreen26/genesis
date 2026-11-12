package shipwrights.genesis.compat.vs;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;
import org.valkyrienskies.mod.common.block.TestHingeBlock;
import org.valkyrienskies.mod.common.blockentity.TestHingeBlockEntity;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/// The tests of {@link ShipTravelGameTests}, apart so they're only loaded with Valkyrien Skies
final class ShipTravelTestCases {

    static void shipLeavesForSpaceWithWhatsAboard(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        ServerShip ship = assembleShip(helper, new BlockPos(1, 1, 1));

        Vector3dc start = ship.getTransform().getPositionInWorld();
        moveShip(overworld, ship, overworld,
                new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() + 10, start.z()),
                new Vector3d(0, 5, 0));

        // A chicken in the ship's own coordinates, as in a seat
        Vector3dc inShip = ship.getTransform().getPositionInShip();
        Chicken chicken = EntityType.CHICKEN.create(overworld);
        helper.assertTrue(chicken != null, "couldn't create a chicken");
        chicken.setNoAi(true);
        chicken.moveTo(inShip.x(), inShip.y() + 1, inShip.z());
        overworld.addFreshEntity(chicken);
        UUID chickenId = chicken.getUUID();
        Vec3 chickenInShip = chicken.position();

        helper.succeedWhen(() -> {
            ServerShip found = shipById(overworld, ship.getId());
            helper.assertTrue(VSGameUtilsKt.getDimensionId(space).equals(found.getChunkClaimDimension()),
                    "ship is in " + found.getChunkClaimDimension());
            Entity arrived = space.getEntity(chickenId);
            helper.assertTrue(arrived != null, "chicken in the ship didn't go with it");
            helper.assertTrue(arrived.position().distanceTo(chickenInShip) < 1,
                    "chicken moved in the ship, to " + arrived.position() + " from " + chickenInShip);

            OBB cube = cube(space);
            OBB shipObb = OBB.fromShip(found.getShipAABB(), found.getShipToWorld());
            double distance = cube.distanceTo(shipObb);
            helper.assertTrue(distance > CelestialTravel.DEPARTURE_DISTANCE - 1
                            && distance < CelestialTravel.DEPARTURE_DISTANCE + 3,
                    "ship arrived " + distance + " blocks from the overworld");

            // Up, which it was moving, is out from the overworld in space
            Vector3d centre = new Vector3d(found.getTransform().getPositionInWorld());
            Vector3d outward = new Vector3d(centre).sub(cube.closestPointTo(centre)).normalize();
            Vector3d velocity = new Vector3d(found.getVelocity());
            helper.assertTrue(velocity.length() > 0.5 && velocity.normalize().dot(outward) > 0.9,
                    "ship isn't moving away from the overworld: " + found.getVelocity());

            arrived.discard();
            ShipAssembler.INSTANCE.deleteShip(space, found, true, false);
        });
    }

    static void shipArrivesFromSpace(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        ServerShip ship = assembleShip(helper, new BlockPos(1, 1, 1));

        // Just off the overworld's north face, to one side of the middle, moving towards it
        Celestial celestial = overworldCelestial(space);
        CelestialTravel.Placement placement = CelestialTravel.placement(space, celestial);
        Quaterniondc rotation = placement.rotation();
        Vector3d start = rotation.transform(new Vector3d(10, 0, -(celestial.size() / 2 + 4))).add(placement.position());
        moveShip(overworld, ship, space, start, rotation.transform(new Vector3d(0, 0, 5)));

        helper.succeedWhen(() -> {
            ServerShip found = shipById(overworld, ship.getId());
            helper.assertTrue(VSGameUtilsKt.getDimensionId(overworld).equals(found.getChunkClaimDimension()),
                    "ship is in " + found.getChunkClaimDimension());
            double top = found.getWorldAABB().maxY();
            helper.assertTrue(top < GenesisCommonConfig.atmosphereExitHeight()
                            && top > CelestialTravel.arrivalHeight() - 20,
                    "ship's top arrived at " + top);
            Vector3dc position = found.getTransform().getPositionInWorld();
            helper.assertTrue(Math.abs(position.x()) < GenesisCommonConfig.worldSize() / 8.0
                            && Math.abs(position.z()) < GenesisCommonConfig.worldSize() / 8.0,
                    "ship arrived at " + position + ", off the north face");
            helper.assertTrue(found.getVelocity().y() < -1, "ship isn't moving down: " + found.getVelocity());

            ShipAssembler.INSTANCE.deleteShip(overworld, found, true, false);
        });
    }

    static void joinedShipsTravelTogether(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);
        ServerShip first = assembleShip(helper, new BlockPos(1, 1, 1));
        long firstId = first.getId();
        AtomicLong secondId = new AtomicLong(-1);

        // VS's test hinge makes a second ship, hinged to the first, once the first is in the physics world
        helper.runAfterDelay(10, () -> {
            Vector3dc inShip = first.getTransform().getPositionInShip();
            BlockPos hinge = new BlockPos((int) (inShip.x() - 0.5), (int) (inShip.y() - 0.5), (int) (inShip.z() - 0.5));
            overworld.setBlockAndUpdate(hinge, ValkyrienSkiesMod.TEST_HINGE.defaultBlockState());
            TestHingeBlockEntity hingeEntity = (TestHingeBlockEntity) overworld.getBlockEntity(hinge);
            helper.assertTrue(hingeEntity != null, "no test hinge");
            TestHingeBlock.INSTANCE.use(ValkyrienSkiesMod.TEST_HINGE.defaultBlockState(), overworld, hinge,
                    FakePlayerFactory.get(overworld, new GameProfile(UUID.randomUUID(), "genesis_test")),
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(hinge), Direction.UP, hinge, false));
            BlockPos otherHinge = hingeEntity.getOtherHingePos();
            helper.assertTrue(otherHinge != null, "test hinge made no second ship");
            ServerShip second = VSGameUtilsKt.getShipManagingPos(overworld, otherHinge);
            helper.assertTrue(second != null, "no second ship");
            secondId.set(second.getId());
        });

        // Once the joint is made, on the physics side, lift the first above the exit height
        helper.runAfterDelay(40, () -> {
            ServerShip lifted = shipById(overworld, firstId);
            Vector3dc start = lifted.getTransform().getPositionInWorld();
            moveShip(overworld, lifted, overworld,
                    new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() + 10, start.z()), new Vector3d());
        });

        String spaceId = VSGameUtilsKt.getDimensionId(space);
        helper.succeedWhen(() -> {
            helper.assertTrue(secondId.get() >= 0, "no second ship yet");
            ServerShip a = shipById(overworld, firstId);
            ServerShip b = shipById(overworld, secondId.get());
            helper.assertTrue(spaceId.equals(a.getChunkClaimDimension()), "first ship is in " + a.getChunkClaimDimension());
            helper.assertTrue(spaceId.equals(b.getChunkClaimDimension()),
                    "hinged ship didn't go with it: it's in " + b.getChunkClaimDimension());
            ShipAssembler.INSTANCE.deleteShip(space, a, true, false);
            ShipAssembler.INSTANCE.deleteShip(space, b, true, false);
        });
    }

    static void staticShipStays(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerShip ship = assembleShip(helper, new BlockPos(1, 1, 1));
        Vector3dc start = ship.getTransform().getPositionInWorld();
        moveShip(overworld, ship, overworld,
                new Vector3d(start.x(), GenesisCommonConfig.atmosphereExitHeight() + 10, start.z()), new Vector3d());
        ship.setStatic(true);

        helper.runAfterDelay(20, () -> {
            ServerShip found = shipById(overworld, ship.getId());
            helper.assertTrue(VSGameUtilsKt.getDimensionId(overworld).equals(found.getChunkClaimDimension()),
                    "static ship travelled to " + found.getChunkClaimDimension());
            ShipAssembler.INSTANCE.deleteShip(overworld, found, true, false);
            helper.succeed();
        });
    }

    /// A ship of a 3×3 stone platform at {@code relative} in the test's structure
    private static ServerShip assembleShip(GameTestHelper helper, BlockPos relative) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(relative);
        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos block = centre.offset(dx, 0, dz);
                level.setBlock(block, Blocks.STONE.defaultBlockState(), 3);
                blocks.add(block);
            }
        }
        return ShipAssembler.INSTANCE.assembleToShip(level, blocks, true, 1.0, false);
    }

    /// Puts {@code ship}, now in {@code from}, at {@code position} in {@code to}, moving at {@code velocity}
    private static void moveShip(ServerLevel from, ServerShip ship, ServerLevel to, Vector3dc position,
                                 Vector3dc velocity) {
        VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(from);
        shipWorld.teleportShip(ship, VSGameUtilsKt.getVsCore().newShipTeleportData(position,
                ship.getTransform().getShipToWorldRotation(), velocity, new Vector3d(), VSGameUtilsKt.getDimensionId(to),
                null, null));
    }

    private static ServerShip shipById(ServerLevel level, long id) {
        ServerShip ship = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(id);
        if (ship == null) throw new IllegalStateException("lost ship " + id);
        return ship;
    }

    private static ServerLevel space(GameTestHelper helper) {
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
