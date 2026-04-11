package shipwrights.genesis.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GenesisClientConfig {

    private static ForgeConfigSpec.ConfigValue<Boolean> spaceShaderEnable;
    private static final boolean defaultSpaceShaderEnable = true;

    private static ForgeConfigSpec.ConfigValue<Boolean> renderCurrentPlanet;
    private static final boolean defaultRenderCurrentPlanet = true;

    public static boolean enableSpaceLighting() {
        boolean result = defaultSpaceShaderEnable;
        try {
            result = spaceShaderEnable.get();
        } catch (Exception ignored) { }
        return result;
    }

    public static boolean shouldRenderCurrentPlanet() {
        boolean result = defaultRenderCurrentPlanet;
        try {
            result = renderCurrentPlanet.get();
        } catch (Exception ignored) { }
        return result;
    }

    public static final ForgeConfigSpec CONFIG_SPEC = buildConfig();

    private static ForgeConfigSpec buildConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        spaceShaderEnable = builder.define("EnableDynamicSpaceLighting", defaultSpaceShaderEnable);
        renderCurrentPlanet = builder.define("ShouldRenderCurrentPlanet", defaultRenderCurrentPlanet);
        return builder.build();
    }
}
