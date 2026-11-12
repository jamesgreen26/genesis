package shipwrights.genesis.compat.sable;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.Compat;

/// Sable sub-levels travelling between the overworld, which is a celestial with a dimension of its own, and space.
/// Pass without Sable.
///
/// NeoForge looks over every method of a test class, signatures and all, to find the tests. So this one names no Sable
/// class, and leaves the tests themselves to {@link SubLevelTravelTestCases}
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SubLevelTravelGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    /// Run apart from the other tests, which run all at once, as sub-levels are solid to the entities other tests send
    /// to and from space, and arrive at much the same places
    private static final String BATCH = "sable_sub_level_travel";

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void subLevelLeavesForSpaceWithWhatsAboard(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.subLevelLeavesForSpaceWithWhatsAboard(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void seatedRiderLeavesForSpace(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.seatedRiderLeavesForSpace(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void subLevelArrivesFromSpace(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.subLevelArrivesFromSpace(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = BATCH)
    public static void noHeightLimitsInSpace(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.noHeightLimitsInSpace(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = BATCH)
    public static void plotsAreUniqueAcrossDimensions(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.plotsAreUniqueAcrossDimensions(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void blockActorsWorkAfterTravel(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.blockActorsWorkAfterTravel(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void reactorTravelsWithSubLevel(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.reactorTravelsWithSubLevel(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void roomTravelsWithSubLevel(GameTestHelper helper) {
        if (withoutSable(helper)) return;
        SubLevelTravelTestCases.roomTravelsWithSubLevel(helper);
    }

    /// Passes the test if Sable isn't loaded
    private static boolean withoutSable(GameTestHelper helper) {
        if (Compat.isSableLoaded()) return false;
        helper.succeed();
        return true;
    }
}
