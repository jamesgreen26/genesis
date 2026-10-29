package shipwrights.genesis;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.network.NetworkHooks;

public final class MinecraftBootstrap {
    private MinecraftBootstrap() {}

    /// Registries can't be constructed until Minecraft has been bootstrapped. Call from a {@code @BeforeAll}.
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError e) {
            // Forge's NetworkHooks.init() is the final bootstrap step and needs ModLauncher's event transformer,
            // which isn't present in unit tests. Vanilla bootstrapping has already completed by then.
            if (!e.getStackTrace()[0].getClassName().equals(NetworkHooks.class.getName())) {
                throw e;
            }
        }
    }
}
