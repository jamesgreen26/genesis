package shipwrights.genesis.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GenesisCommonConfig {
    private static ForgeConfigSpec.ConfigValue<Integer> atmosphereExitHeight;
    private static ForgeConfigSpec.ConfigValue<Integer> atmosphereEntryHeight;

    public static final ForgeConfigSpec CONFIG_SPEC = buildConfig();

    private static ForgeConfigSpec buildConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        atmosphereExitHeight = builder.define("atmosphereExitHeight", 2048);
        atmosphereEntryHeight = builder.define("atmosphereEntryHeight",1440);
        return builder.build();
    }

    public static int getAtmosphereExitHeight() {
        return atmosphereExitHeight.get();
    }

    public static int getAtmosphereEntryHeight() {
        return atmosphereEntryHeight.get();
    }
}
