package shipwrights.genesis.compat.sable;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.Compat;

/// Simulated's physics staff, which Create Aeronautics comes with, far out in space: see
/// {@link shipwrights.genesis.compat.simulated.mixin.PhysicsStaffDragSessionMixin}. Pass without Sable or Simulated,
/// which a run has only when started with {@code -Paeronautics}.
///
/// Names no class of either, leaving the tests to {@link PhysicsStaffTestCases}
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class PhysicsStaffGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    /// Each is run apart from every other test, as their sub-levels are all made at the same place
    private static final String BATCH = "simulated_physics_staff_";

    @GameTest(template = TEMPLATE, timeoutTicks = 300, batch = BATCH + "staffLocksInPlaceFarOut")
    public static void staffLocksInPlaceFarOut(GameTestHelper helper) {
        if (without(helper)) return;
        PhysicsStaffTestCases.staffLocksInPlaceFarOut(helper);
    }

    /// Passes the test if Sable or Simulated isn't loaded
    private static boolean without(GameTestHelper helper) {
        if (Compat.isSableLoaded() && ModList.get().isLoaded("simulated")) return false;
        helper.succeed();
        return true;
    }
}
