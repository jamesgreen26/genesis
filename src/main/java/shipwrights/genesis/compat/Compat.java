package shipwrights.genesis.compat;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModList;
import shipwrights.genesis.compat.vs.VsCompat;

/// Where Genesis works with other mods it doesn't need. Each has a package of its own under this one, the only code
/// that may use its classes, which is only ever reached once it's known to be loaded
public final class Compat {

    private Compat() {
    }

    public static boolean isVsLoaded() {
        return ModList.get().isLoaded("valkyrienskies");
    }

    /// Called as Genesis is constructed
    public static void init() {
        if (isVsLoaded()) {
            VsCompat.init(NeoForge.EVENT_BUS);
        }
    }
}
