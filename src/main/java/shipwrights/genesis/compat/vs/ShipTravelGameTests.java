package shipwrights.genesis.compat.vs;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.Compat;

/// Valkyrien Skies ships travelling between the overworld, which is a celestial with a dimension of its own, and space.
/// Pass without Valkyrien Skies.
///
/// Forge looks over every method of a test class, signatures and all, to find the tests. So this one names no Valkyrien
/// Skies class, and leaves the tests themselves to {@link ShipTravelTestCases}
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ShipTravelGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void shipLeavesForSpaceWithWhatsAboard(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.shipLeavesForSpaceWithWhatsAboard(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void shipArrivesFromSpace(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.shipArrivesFromSpace(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void joinedShipsTravelTogether(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.joinedShipsTravelTogether(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void staticShipStays(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.staticShipStays(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void reactorTravelsWithShip(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.reactorTravelsWithShip(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void roomTravelsWithShip(GameTestHelper helper) {
        if (withoutVs(helper)) return;
        ShipTravelTestCases.roomTravelsWithShip(helper);
    }

    /// Passes the test if Valkyrien Skies isn't loaded
    private static boolean withoutVs(GameTestHelper helper) {
        if (Compat.isVsLoaded()) return false;
        helper.succeed();
        return true;
    }
}
