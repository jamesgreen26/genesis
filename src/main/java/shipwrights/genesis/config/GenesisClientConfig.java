package shipwrights.genesis.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GenesisClientConfig {

    private static ForgeConfigSpec.ConfigValue<Boolean> spaceShaderEnable;
    private static final boolean defaultSpaceShaderEnable = true;

    public static boolean enableSpaceLighting() {
        boolean result = defaultSpaceShaderEnable;
        try {
            result = spaceShaderEnable.get();
        } catch (Exception ignored) { }
        return result;
    }

    public static final ForgeConfigSpec CONFIG_SPEC = buildConfig();

    private static ForgeConfigSpec buildConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        spaceShaderEnable = builder.define("EnableDynamicSpaceLighting", defaultSpaceShaderEnable);
        return builder.build();
    }
}
