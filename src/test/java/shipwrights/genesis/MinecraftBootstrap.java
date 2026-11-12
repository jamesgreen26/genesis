package shipwrights.genesis;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class MinecraftBootstrap {
    private MinecraftBootstrap() {}

    /// Registries can't be constructed until Minecraft has been bootstrapped. Call from a {@code @BeforeAll}.
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError e) {
            // NeoForge's own bootstrap steps come last and need the mod loader, which isn't present in unit tests.
            // Vanilla bootstrapping has already completed by then.
            if (!e.getStackTrace()[0].getClassName().startsWith("net.neoforged.")) {
                throw e;
            }
        }
    }
}
