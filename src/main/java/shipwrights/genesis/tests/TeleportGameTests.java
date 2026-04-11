package shipwrights.genesis.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;
import org.valkyrienskies.mod.common.block.TestHingeBlock;
import org.valkyrienskies.mod.common.blockentity.TestHingeBlockEntity;
import org.joml.primitives.AABBic;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.Celestial;
import java.util.UUID;

/**
 * Forge GameTests for the Genesis ship teleportation system.
 *
 * <p>Tests assert <em>only</em> on observable outcomes — specifically which dimension a ship or
 * entity ends up in. No internal state (LAUNCHING/LANDING maps, DimensionTravelTeleporter fields,
 * velocity values) is inspected, so tests remain valid across teleportation redesigns.
 *
 * <p>Ship assembly calls {@link ShipAssembler} directly via {@link TestShipHelper#assembleShip},
 * which returns the {@link LoadedServerShip} synchronously. Tests no longer need a polling phase
 * to discover the assembled ship.
 */
@GameTestHolder(GenesisMod.MOD_ID)
public class TeleportGameTests {

    private static final ResourceKey<net.minecraft.world.level.Level> SPACE_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM);

    /** Relative position within the test structure where the ship platform is placed. */
    private static final BlockPos SHIP_ASSEMBLY_REL_POS = new BlockPos(2, 0, 2);

    // -----------------------------------------------------------------------
    // § 2 – Scaffold
    // -----------------------------------------------------------------------

    /**
     * Smoke test: the GameTest framework is configured correctly and can discover tests in the
     * {@code genesis} namespace.
     */
    @GameTest
    public static void emptyPlatform(GameTestHelper helper) {
        helper.succeed();
    }

    // -----------------------------------------------------------------------
    // § 4 – Atmosphere Exit
    // -----------------------------------------------------------------------

    /**
     * Verifies that a VS ship positioned above {@code GenesisMod.atmosphereExitHeight} in a
     * planet dimension is moved to the space dimension within 100 ticks.
     */
    @GameTest(timeoutTicks = 100)
    public static void atmosphereExit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ServerShip ship = TestShipHelper.assembleShip(helper, SHIP_ASSEMBLY_REL_POS);

        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension '" + GenesisMod.SPACE_DIM + "' not loaded");
            return;
        }
        String expectedSpaceDim = VSGameUtilsKt.getDimensionId(spaceLevel);

        TestShipHelper.moveShipAboveAtmosphere(level, ship);
        long shipId = ship.getId();

        GenesisMod.LOGGER.info("[atmosphereExit] Ship id={} moved above atmosphere ({}); polling for teleport to '{}'",
                shipId, GenesisCommonConfig.getAtmosphereExitHeight() + 20, expectedSpaceDim);

        helper.succeedWhen(() -> {
            ServerShip found = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(shipId);
            if (found == null) {
                throw new GameTestAssertException("Ship record lost for id=" + shipId);
            }
            GenesisMod.LOGGER.debug("[atmosphereExit] tick check: ship id={} chunkClaimDim='{}'",
                    shipId, found.getChunkClaimDimension());
            if (!expectedSpaceDim.equals(found.getChunkClaimDimension())) {
                throw new GameTestAssertException(
                        "Ship not in space yet; chunkClaimDimension='" + found.getChunkClaimDimension()
                                + "' (expected='" + expectedSpaceDim + "')");
            }
            AABBic shipAABB = found.getShipAABB();
            if (shipAABB != null) {
                Celestial celestial = GenesisMod.getCelestialForLevel(level);
                if (celestial != null) {
                    long ticks = GenesisMod.getTicks(spaceLevel);
                    Registry<Celestial> registry = GenesisMod.getCelestialRegistry(spaceLevel);
                    OBB shipOBB = OBB.fromShip(shipAABB, found.getShipToWorld());
                    if (shipOBB.overlapsWith(celestial.getOBB(ticks, registry))) {
                        throw new GameTestAssertException(
                                "Ship OBB overlaps celestial OBB after atmosphere exit (ping-pong risk)");
                    }
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // § 5 – Planet Entry
    // -----------------------------------------------------------------------

    /**
     * Verifies that a VS ship in the space dimension positioned within the collision radius of
     * the {@code minecraft:overworld} celestial body is moved to the overworld within 100 ticks.
     *
     * <p>The {@code minecraft:overworld} celestial is pre-registered via
     * {@code data/genesis/system_config/builtin.json} and will be available after the resource
     * manager reloads on server start. No additional test-specific celestial registration is
     * required.
     */
    @GameTest(timeoutTicks = 100)
    public static void planetEntry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ServerShip ship = TestShipHelper.assembleShip(helper, SHIP_ASSEMBLY_REL_POS);

        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension not loaded");
            return;
        }
        ServerLevel overworldLevel = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworldLevel == null) {
            helper.fail("Overworld not loaded");
            return;
        }
        String expectedOverworldDim = VSGameUtilsKt.getDimensionId(overworldLevel);

        // Move ship into space near the overworld celestial's position
        TestShipHelper.moveShipNearPlanet(spaceLevel, ship, ResourceLocation.parse("minecraft:overworld"));
        long shipId = ship.getId();

        GenesisMod.LOGGER.info("[planetEntry] Ship id={} moved to space near overworld; polling for planet entry to '{}'",
                shipId, expectedOverworldDim);

        helper.succeedWhen(() -> {
            ServerShip found = VSGameUtilsKt.getShipObjectWorld(spaceLevel).getAllShips().getById(shipId);
            if (found == null) {
                throw new GameTestAssertException("Ship record lost for id=" + shipId);
            }
            GenesisMod.LOGGER.debug("[planetEntry] tick check: ship id={} chunkClaimDim='{}'",
                    shipId, found.getChunkClaimDimension());
            if (!expectedOverworldDim.equals(found.getChunkClaimDimension())) {
                throw new GameTestAssertException(
                        "Ship not in overworld yet; chunkClaimDimension='" + found.getChunkClaimDimension() + "'");
            }
        });
    }

    // -----------------------------------------------------------------------
    // § 6 – Entity Collection
    // -----------------------------------------------------------------------

    /**
     * Verifies that a non-player entity (Pig) positioned within collection range of a ship that
     * exits the atmosphere is moved to the space dimension together with the ship.
     *
     * <p>Asserts that the pig's {@link net.minecraft.world.entity.Entity.RemovalReason} in the
     * source dimension is {@code CHANGED_DIMENSION}, confirming the game's own cross-dimension
     * teleport path was taken.
     */
    @GameTest(timeoutTicks = 100)
    public static void entityTeleportsWithShip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ServerShip ship = TestShipHelper.assembleShip(helper, SHIP_ASSEMBLY_REL_POS);

        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension not loaded");
            return;
        }

        // Spawn a Pig within entity collection range (≤ 8 blocks from ship AABB)
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) {
            helper.fail("Could not create Pig entity");
            return;
        }
        Vector3dc shipPos = ship.getTransform().getPositionInWorld();
        pig.moveTo(shipPos.x() + 2, shipPos.y() + 1, shipPos.z(), 0, 0);
        level.addFreshEntity(pig);

        TestShipHelper.moveShipAboveAtmosphere(level, ship);
        long shipId = ship.getId();

        GenesisMod.LOGGER.info("[entityTeleportsWithShip] Ship id={} and pig UUID={} above atmosphere; polling for entity teleport",
                shipId, pig.getStringUUID());

        helper.succeedWhen(() -> {
            GenesisMod.LOGGER.debug("[entityTeleportsWithShip] tick check: pig removalReason={}",
                    pig.getRemovalReason());
            if (!net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION.equals(pig.getRemovalReason())) {
                throw new GameTestAssertException(
                        "Pig not yet moved to another dimension; removalReason=" + pig.getRemovalReason());
            }
        });
    }

    // -----------------------------------------------------------------------
    // § 7 – Connected Ships
    // -----------------------------------------------------------------------

    /**
     * Verifies that two VS ships connected by a VS joint both move to the space dimension when
     * ship A exits the atmosphere.
     *
     * <p>A {@link TestHingeBlock} is placed on ship A and right-clicked to create ship B and a
     * {@link org.valkyrienskies.core.internal.joints.VSRevoluteJoint} between the two ships.
     * The test then waits for the joint to register in the physics world before launching ship A
     * above the atmosphere, and asserts that both ships end up in the space dimension.
     */
    @GameTest(timeoutTicks = 200)
    public static void connectedShipsTeleportTogether(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Assemble ship A from a 3×3 stone platform.
        ServerShip shipA = TestShipHelper.assembleShip(helper, new BlockPos(1, 0, 1));
        long shipAId = shipA.getId();

        // After assembly the ship's blocks live at ship-space ("shipyard") coordinates,
        // NOT at the original overworld positions. getShipManagingPos checks isChunkInShipyard,
        // which is only true for shipyard-chunk coordinates. We compute ship A's center block
        // position in ship-space (the same formula TestHingeBlock uses for ship B's iron block)
        // and place the hinge there so getShipManagingPos returns ship A inside use().
        org.joml.Vector3dc shipAInShipPos = shipA.getTransform().getPositionInShip();
        BlockPos hingeWorldPos = new BlockPos(
                (int)(shipAInShipPos.x() - 0.5),
                (int)(shipAInShipPos.y() - 0.5),
                (int)(shipAInShipPos.z() - 0.5));
        level.setBlockAndUpdate(hingeWorldPos, ValkyrienSkiesMod.TEST_HINGE.defaultBlockState());

        TestHingeBlockEntity hingeBlockEntity =
                (TestHingeBlockEntity) level.getBlockEntity(hingeWorldPos);
        if (hingeBlockEntity == null) {
            helper.fail("TestHingeBlockEntity not created at " + hingeWorldPos);
            return;
        }

        // Right-click the hinge block. TestHingeBlock.use() will:
        //   1. detect it is on ship A via getShipManagingPos
        //   2. create a new ship (ship B) at an adjacent position
        //   3. place an iron block in ship B and set blockEntity.otherHingePos
        //   4. queue a VSRevoluteJoint between ship A and ship B (GTPA delay = 4 phys ticks)
        // A FakePlayer is required by the method signature but its state is not examined.
        TestHingeBlock.INSTANCE.use(
                ValkyrienSkiesMod.TEST_HINGE.defaultBlockState(),
                level,
                hingeWorldPos,
                FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "genesis_test")),
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(hingeWorldPos.getX() + 0.5, hingeWorldPos.getY() + 0.5, hingeWorldPos.getZ() + 0.5),
                        Direction.UP,
                        hingeWorldPos,
                        false));

        // Resolve ship B from the position the hinge block entity stored.
        BlockPos shipBPos = hingeBlockEntity.getOtherHingePos();
        if (shipBPos == null) {
            helper.fail("TestHingeBlock.use() did not set otherHingePos; joint creation may have failed");
            return;
        }
        ServerShip shipBRef = VSGameUtilsKt.getShipManagingPos(level, shipBPos);
        if (shipBRef == null) {
            helper.fail("Could not find ship B managing pos=" + shipBPos);
            return;
        }
        long shipBId = shipBRef.getId();

        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension not loaded");
            return;
        }
        String expectedSpaceDim = VSGameUtilsKt.getDimensionId(spaceLevel);

        // Wait for the joint to be fully registered in the physics world (GTPA delay = 4 phys
        // ticks; 30 game ticks gives plenty of margin), then launch ship A.
        helper.runAfterDelay(30, () -> {
            ServerShip shipALookup = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(shipAId);
            if (shipALookup == null) {
                helper.fail("Ship A record lost before launch");
                return;
            }
            GenesisMod.LOGGER.info("[connectedShipsTeleportTogether] launching shipA id={} above atmosphere; shipB id={}",
                    shipAId, shipBId);
            TestShipHelper.moveShipAboveAtmosphere(level, shipALookup);
        });

        helper.succeedWhen(() -> {
            ServerShip foundA = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(shipAId);
            ServerShip foundB = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(shipBId);
            if (foundA == null || foundB == null) {
                throw new GameTestAssertException(
                        "Ship record(s) lost: shipA=" + shipAId + " shipB=" + shipBId);
            }
            GenesisMod.LOGGER.debug("[connectedShipsTeleportTogether] shipA dim='{}' shipB dim='{}'",
                    foundA.getChunkClaimDimension(), foundB.getChunkClaimDimension());
            if (!expectedSpaceDim.equals(foundA.getChunkClaimDimension())) {
                throw new GameTestAssertException("Ship A not in space: " + foundA.getChunkClaimDimension());
            }
            if (!expectedSpaceDim.equals(foundB.getChunkClaimDimension())) {
                throw new GameTestAssertException("Ship B not in space: " + foundB.getChunkClaimDimension());
            }
        });
    }
}
