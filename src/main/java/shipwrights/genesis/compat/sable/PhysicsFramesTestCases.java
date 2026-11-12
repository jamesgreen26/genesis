package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/// The tests of {@link PhysicsFramesGameTests}, which names no Sable class.
///
/// All are far enough from space's origin that, were Sable's physics engine told they were there, nothing could move
/// slower than a block and a half a second, nor at any speed between multiples of nearly three
final class PhysicsFramesTestCases {

    /// Half as far out as the overworld
    private static final Vector3dc FAR = new Vector3d(60_000.5, 64, 30_000.5);

    /// Where the tests' sub-levels are made, before they're moved out: far from every celestial
    private static final BlockPos ASSEMBLY = new BlockPos(30_000, 64, 30_000);

    /// Slower than anything can move a thousand blocks from the engine's origin, let alone this far
    private static final double SLOW = 0.03;

    private PhysicsFramesTestCases() {
    }

    /// For longer than the two seconds after which Sable would stop whatever is this slow, but for {@link Drift}
    static void slowDriftIsExactFarOut(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ServerSubLevel subLevel = subLevel(helper, space, 0, FAR, new Vector3d(SLOW, 0, 0));

        int ticks = 100;
        helper.runAfterDelay(ticks, () -> {
            assertMoved(helper, subLevel, FAR, new Vector3d(SLOW, 0, 0), ticks);
            assertNearEngineOrigin(helper, subLevel);
            helper.succeed();
        });
    }

    /// As players at different celestials would be. Only one frame can be at the engine's origin: in the next, speeds
    /// come in steps of about a hundredth of a block a second
    static void farApartAreBothExact(GameTestHelper helper) {
        ServerLevel space = space(helper);
        Vector3d elsewhere = new Vector3d(-45_000.5, 64, 9000.5);
        Vector3d docking = new Vector3d(0, 0, -0.2);
        ServerSubLevel first = subLevel(helper, space, 0, FAR, new Vector3d(SLOW, 0, 0));
        ServerSubLevel second = subLevel(helper, space, 1, elsewhere, docking);

        int ticks = 100;
        helper.runAfterDelay(ticks, () -> {
            assertMoved(helper, first, FAR, new Vector3d(SLOW, 0, 0), ticks);
            assertMoved(helper, second, elsewhere, docking, ticks, 0.05);
            assertNearEngineOrigin(helper, first);
            assertNearEngineOrigin(helper, second);
            helper.assertTrue(!PhysicsFrames.shareFrame(first, second), "they share a frame, 100,000 blocks apart");
            helper.succeed();
        });
    }

    /// Two joined sub-levels flying far enough to be brought back to the engine's origin half a dozen times, between
    /// one step of the engine and the next
    static void keepsUpWithFastFlight(GameTestHelper helper) {
        ServerLevel space = space(helper);
        Vector3d velocity = new Vector3d(0, 0, 40);
        ServerSubLevel first = subLevel(helper, space, 0, FAR, new Vector3d());
        Vector3d secondAt = new Vector3d(FAR).add(6, 0, 0);
        ServerSubLevel second = subLevel(helper, space, 1, secondAt, new Vector3d());
        join(helper, space, first, second, FAR);
        PhysicsPipeline pipeline = pipeline(space);
        pipeline.addLinearAndAngularVelocity(first, velocity, new Vector3d());
        pipeline.addLinearAndAngularVelocity(second, velocity, new Vector3d());

        int ticks = 100;
        helper.runAfterDelay(ticks, () -> {
            assertMoved(helper, first, FAR, velocity, ticks);
            assertMoved(helper, second, secondAt, velocity, ticks);
            double apart = first.logicalPose().position().distance(second.logicalPose().position());
            helper.assertTrue(Math.abs(apart - 6) < 0.05, "they're " + apart + " blocks apart, not 6");
            assertNearEngineOrigin(helper, first);
            helper.assertTrue(PhysicsFrames.shareFrame(first, second), "they don't share a frame");
            helper.succeed();
        });
    }

    static void subLevelsCollideFarOut(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ServerSubLevel resting = subLevel(helper, space, 0, FAR, new Vector3d());
        // Each is three blocks across, so three apart
        ServerSubLevel moving = subLevel(helper, space, 1, new Vector3d(FAR).sub(6, 0, 0), new Vector3d(2, 0, 0));

        helper.runAfterDelay(60, () -> {
            assertHit(helper, resting, moving);
            helper.succeed();
        });
    }

    /// From further apart than sub-levels share a frame, so they come to share one on the way
    static void meetingFromApartCollide(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ServerSubLevel resting = subLevel(helper, space, 0, FAR, new Vector3d());
        ServerSubLevel moving = subLevel(helper, space, 1, new Vector3d(FAR).sub(300, 0, 0), new Vector3d(40, 0, 0));

        helper.runAfterDelay(5, () -> helper.assertTrue(!PhysicsFrames.shareFrame(resting, moving),
                "they share a frame, 300 blocks apart"));
        helper.runAfterDelay(200, () -> {
            assertHit(helper, resting, moving);
            helper.succeed();
        });
    }

    static void partingGoSeparateWays(GameTestHelper helper) {
        ServerLevel space = space(helper);
        Vector3d velocity = new Vector3d(-20, 0, 0);
        ServerSubLevel staying = subLevel(helper, space, 0, FAR, new Vector3d());
        Vector3d leavingFrom = new Vector3d(FAR).sub(6, 0, 0);
        ServerSubLevel leaving = subLevel(helper, space, 1, leavingFrom, velocity);

        helper.runAfterDelay(5, () -> helper.assertTrue(PhysicsFrames.shareFrame(staying, leaving),
                "they don't share a frame, three blocks apart"));
        int ticks = 160;
        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(!PhysicsFrames.shareFrame(staying, leaving), "they still share a frame, 160 blocks apart");
            assertMoved(helper, leaving, leavingFrom, velocity, ticks);
            assertNearEngineOrigin(helper, staying);
            assertNearEngineOrigin(helper, leaving);
            helper.succeed();
        });
    }

    /// What's held to the world is held where it is in space, though the engine has it near its own origin, and is
    /// still held there when its frame is taken into another's, and when it's given one of its own again: here by
    /// three joined sub-levels flying past
    static void heldToWorldStaysPut(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ServerSubLevel subLevel = subLevel(helper, space, 0, FAR, new Vector3d());
        FixedConstraintHandle joint = pipeline(space).addConstraint(subLevel, null, new FixedConstraintConfiguration(
                new Vector3d(subLevel.logicalPose().rotationPoint()), new Vector3d(FAR), new Quaterniond()));
        helper.assertTrue(joint != null, "it wasn't held");
        helper.assertTrue(!PhysicsFrames.inWorldFrame(subLevel), "held to the world, it's in the world's frame");
        pipeline(space).addLinearAndAngularVelocity(subLevel, new Vector3d(0, 0, 3), new Vector3d());

        // Past it 24 blocks off, from 200 blocks away
        Vector3d velocity = new Vector3d(40, 0, 0);
        Vector3d passingFrom = new Vector3d(FAR).add(-200, 0, 24);
        ServerSubLevel[] passing = new ServerSubLevel[3];
        for (int i = 0; i < passing.length; i++) {
            passing[i] = subLevel(helper, space, i + 1, new Vector3d(passingFrom).add(0, 0, i * 6), new Vector3d());
            if (i > 0) join(helper, space, passing[0], passing[i], passingFrom);
        }
        for (ServerSubLevel one : passing) pipeline(space).addLinearAndAngularVelocity(one, velocity, new Vector3d());

        boolean[] shared = {false};
        helper.onEachTick(() -> {
            if (subLevel.isRemoved() || passing[0].isRemoved()) return;
            shared[0] |= PhysicsFrames.shareFrame(subLevel, passing[0]);
            double moved = subLevel.logicalPose().position().distance(FAR);
            helper.assertTrue(moved < 0.1, "held to the world, it's " + moved + " blocks from where it's held");
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(shared[0], "it never shared a frame with what passed it");
            helper.assertTrue(!PhysicsFrames.shareFrame(subLevel, passing[0]), "it still shares a frame with what passed it");
            assertNearEngineOrigin(helper, subLevel);
            joint.remove();
            helper.succeed();
        });
    }

    /// As Simulated's physics staff drags a sub-level, with Genesis's change to it (see
    /// {@link shipwrights.genesis.compat.simulated.mixin.PhysicsStaffDragSessionMixin}): by a joint to the origin of
    /// the sub-level's frame, driven to where in the frame it should be. Ten million blocks out, where a position in
    /// the level only comes in whole blocks to the engine
    static void draggedToWhereItShouldBe(GameTestHelper helper) {
        ServerLevel space = space(helper);
        Vector3d start = new Vector3d(10_000_000.5, 64, 30_000.5);
        ServerSubLevel subLevel = subLevel(helper, space, 0, start, new Vector3d());
        // By steps finer than a position this far out can be told to the engine at all
        Vector3d goal = new Vector3d(start).add(0, 0.3, 0);
        double mass = subLevel.getMassTracker().getMass();
        FreeConstraintHandle[] joint = {null};
        helper.onEachTick(() -> {
            if (subLevel.isRemoved()) return;
            // Made again whenever the engine has let go of it, as the staff does
            if (joint[0] == null || !joint[0].isValid()) {
                boolean was = PhysicsFrames.localJoints(true);
                try {
                    joint[0] = pipeline(space).addConstraint(null, subLevel, new FreeConstraintConfiguration(
                            new Vector3d(), new Vector3d(subLevel.logicalPose().rotationPoint()), new Quaterniond()));
                } finally {
                    PhysicsFrames.localJoints(was);
                }
            }
            goal.add(0.011, 0, 0.007);
            Vector3d inFrame = new Vector3d(goal).sub(PhysicsFrames.offset(space, subLevel.getRuntimeId()));
            joint[0].setMotor(ConstraintJointAxis.LINEAR_X, inFrame.x, 400 * mass, 40 * mass, false, 0);
            joint[0].setMotor(ConstraintJointAxis.LINEAR_Y, inFrame.y, 400 * mass, 40 * mass, false, 0);
            joint[0].setMotor(ConstraintJointAxis.LINEAR_Z, inFrame.z, 400 * mass, 40 * mass, false, 0);
        });
        helper.runAfterDelay(100, () -> {
            double from = subLevel.logicalPose().position().distance(goal);
            helper.assertTrue(from < 0.05, "it's " + from + " blocks from where it's dragged to");
            helper.assertTrue(!PhysicsFrames.inWorldFrame(subLevel), "dragged, it's in the world's frame");
            if (joint[0].isValid()) joint[0].remove();
            helper.succeed();
        });
    }

    /// A box, which stays in the world's frame, hits a sub-level, which joins it there until the box is gone
    static void boxJoinsWhatItCanTouch(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ServerSubLevel subLevel = subLevel(helper, space, 0, FAR, new Vector3d());
        SubLevelPhysicsSystem system = SubLevelContainer.getContainer(space).physicsSystem();

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!PhysicsFrames.inWorldFrame(subLevel), "it's in the world's frame, with nothing near");
            Pose3d pose = new Pose3d();
            pose.position().set(FAR).sub(8, -0.5, 0);
            BoxPhysicsObject box = new BoxPhysicsObject(pose, new Vector3d(1, 1, 1), 500);
            system.addObject(box);
            helper.assertTrue(PhysicsFrames.inWorldFrame(subLevel), "it isn't in the world's frame, with a box near");
            pipeline(space).addLinearAndAngularVelocity(box, new Vector3d(4, 0, 0), new Vector3d());

            helper.runAfterDelay(60, () -> {
                double pushed = subLevel.logicalPose().position().x() - FAR.x();
                helper.assertTrue(pushed > 0.1, "hit by a box, it moved " + pushed + " blocks");
                system.removeObject(box);
            });
            helper.runAfterDelay(65, () -> {
                helper.assertTrue(!PhysicsFrames.inWorldFrame(subLevel), "it's still in the world's frame, the box gone");
                helper.succeed();
            });
        });
    }

    private static void assertHit(GameTestHelper helper, ServerSubLevel resting, ServerSubLevel moving) {
        double pushed = resting.logicalPose().position().x() - FAR.x();
        helper.assertTrue(pushed > 0.1, "what was hit moved " + pushed + " blocks");
        double gap = resting.logicalPose().position().x() - moving.logicalPose().position().x();
        helper.assertTrue(gap > 2.9, "they're " + gap + " blocks apart, one inside the other");
    }

    /// {@code subLevel} has gone as far from {@code start} as {@code velocity}, in blocks a second, takes it in
    /// {@code ticks}, to within a fiftieth
    private static void assertMoved(GameTestHelper helper, ServerSubLevel subLevel, Vector3dc start, Vector3dc velocity,
                                    int ticks) {
        assertMoved(helper, subLevel, start, velocity, ticks, 0.02);
    }

    /// As {@link #assertMoved(GameTestHelper, ServerSubLevel, Vector3dc, Vector3dc, int)}, to within {@code tolerance}
    /// of how far that is
    private static void assertMoved(GameTestHelper helper, ServerSubLevel subLevel, Vector3dc start, Vector3dc velocity,
                                    int ticks, double tolerance) {
        Vector3d moved = new Vector3d(subLevel.logicalPose().position()).sub(start);
        // Its pose is from the tick before
        Vector3d expected = new Vector3d(velocity).mul((ticks - 1) / 20.0);
        helper.assertTrue(moved.distance(expected) < expected.length() * tolerance,
                "at " + velocity + " blocks a second it moved " + moved + " blocks, not " + expected);
    }

    private static void assertNearEngineOrigin(GameTestHelper helper, ServerSubLevel subLevel) {
        Vector3d inEngine = PhysicsFrames.inEngine(subLevel);
        helper.assertTrue(inEngine.length() < 400, "the engine has it at " + inEngine);
    }

    /// Holds {@code second} to {@code first}, at {@code at} in space
    private static void join(GameTestHelper helper, ServerLevel space, ServerSubLevel first, ServerSubLevel second,
                             Vector3dc at) {
        helper.assertTrue(pipeline(space).addConstraint(first, second, new FixedConstraintConfiguration(
                first.logicalPose().transformPositionInverse(new Vector3d(at)),
                second.logicalPose().transformPositionInverse(new Vector3d(at)), new Quaterniond())) != null,
                "they weren't joined");
    }

    private static PhysicsPipeline pipeline(ServerLevel space) {
        return SubLevelContainer.getContainer(space).physicsSystem().getPipeline();
    }

    private static ServerLevel space(GameTestHelper helper) {
        ServerLevel space = SubLevelTravelTestCases.space(helper);
        SubLevelTravelTestCases.forceLoad(helper, space, ASSEMBLY, 2);
        return space;
    }

    /// A sub-level of {@code helper}'s test, removed when it's over: the {@code index}th it makes, at
    /// {@code position} and moving at {@code velocity} blocks a second
    private static ServerSubLevel subLevel(GameTestHelper helper, ServerLevel space, int index, Vector3dc position,
                                           Vector3dc velocity) {
        ServerSubLevel subLevel = SubLevelTravelTestCases.assemble(space, ASSEMBLY.offset(index * 6, 0, 0),
                SubLevelTravelTestCases.testName(helper), false);
        SubLevelTravelTestCases.moveSubLevel(space, subLevel, position, velocity);
        return subLevel;
    }
}
