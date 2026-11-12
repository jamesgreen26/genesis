package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

/// The tests of {@link PhysicsStaffGameTests}, which names no Sable class. Simulated's are reached by name, as
/// Genesis isn't built against it.
///
/// Only its lock is tried here. Dragging needs a player in space, and Simulated tells every player there of what's
/// dragged, which a player made for a test can't be told. How it drags, with Genesis's change to it, is tried by
/// {@link PhysicsFramesTestCases#draggedToWhereItShouldBe} instead.
///
/// Ten million blocks out, where a position in the level only comes in whole blocks to Sable's physics engine: so
/// everything here is half a block from the nearest
final class PhysicsStaffTestCases {

    private static final Vector3dc FAR = new Vector3d(10_000_000.5, 64.5, 3000.5);

    private static final BlockPos ASSEMBLY = new BlockPos(3000, 64, 3000);

    private static final String HANDLER = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler";

    private PhysicsStaffTestCases() {
    }

    /// Locked, it stays where it is, to well within a block
    static void staffLocksInPlaceFarOut(GameTestHelper helper) {
        ServerLevel space = SubLevelTravelTestCases.space(helper);
        SubLevelTravelTestCases.forceLoad(helper, space, ASSEMBLY, 2);
        ServerSubLevel subLevel = subLevel(helper, space);
        Object staff = staff(space);

        helper.runAfterDelay(5, () -> {
            Vector3d lockedAt = new Vector3d(subLevel.logicalPose().position());
            toggleLock(staff, subLevel.getUniqueId());
            dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).physicsSystem().getPipeline()
                    .addLinearAndAngularVelocity(subLevel, new Vector3d(0, 0, 3), new Vector3d());
            helper.runAfterDelay(60, () -> {
                double moved = subLevel.logicalPose().position().distance(lockedAt);
                toggleLock(staff, subLevel.getUniqueId());
                helper.assertTrue(moved < 0.1, "locked by the staff, it moved " + moved + " blocks");
                helper.assertTrue(!PhysicsFrames.inWorldFrame(subLevel), "locked by the staff, it's in the world's frame");
                helper.succeed();
            });
        });
    }

    private static ServerSubLevel subLevel(GameTestHelper helper, ServerLevel space) {
        ServerSubLevel subLevel = SubLevelTravelTestCases.assemble(space, ASSEMBLY,
                SubLevelTravelTestCases.testName(helper), false);
        SubLevelTravelTestCases.moveSubLevel(space, subLevel, FAR, new Vector3d());
        return subLevel;
    }

    private static Object staff(ServerLevel level) {
        try {
            return Class.forName(HANDLER).getMethod("get", ServerLevel.class).invoke(null, level);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void toggleLock(Object staff, UUID subLevel) {
        try {
            staff.getClass().getMethod("toggleLock", UUID.class).invoke(staff, subLevel);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
