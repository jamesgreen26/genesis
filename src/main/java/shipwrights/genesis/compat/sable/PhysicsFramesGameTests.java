package shipwrights.genesis.compat.sable;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.Compat;

/// Sub-levels far out in space move as they're meant to, and as they would near its origin: see
/// {@link PhysicsFrames}. Pass without Sable.
///
/// Names no Sable class, as {@link SubLevelTravelGameTests} doesn't, leaving the tests to
/// {@link PhysicsFramesTestCases}
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class PhysicsFramesGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    /// Each is run apart from every other test, as their sub-levels are all made at the same place
    private static final String BATCH = "sable_physics_frames_";

    @GameTest(template = TEMPLATE, timeoutTicks = 140, batch = BATCH + "slowDriftIsExactFarOut")
    public static void slowDriftIsExactFarOut(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.slowDriftIsExactFarOut(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140, batch = BATCH + "farApartAreBothExact")
    public static void farApartAreBothExact(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.farApartAreBothExact(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160, batch = BATCH + "keepsUpWithFastFlight")
    public static void keepsUpWithFastFlight(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.keepsUpWithFastFlight(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH + "subLevelsCollideFarOut")
    public static void subLevelsCollideFarOut(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.subLevelsCollideFarOut(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300, batch = BATCH + "meetingFromApartCollide")
    public static void meetingFromApartCollide(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.meetingFromApartCollide(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 240, batch = BATCH + "partingGoSeparateWays")
    public static void partingGoSeparateWays(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.partingGoSeparateWays(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH + "heldToWorldStaysPut")
    public static void heldToWorldStaysPut(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.heldToWorldStaysPut(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH + "boxJoinsWhatItCanTouch")
    public static void boxJoinsWhatItCanTouch(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        PhysicsFramesTestCases.boxJoinsWhatItCanTouch(helper);
    }

    /// Passes the test if Sable isn't loaded
    private static boolean withoutSable(GameTestHelper helper) {
        if (Compat.isSableLoaded()) return false;
        helper.succeed();
        return true;
    }
}
