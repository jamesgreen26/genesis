package shipwrights.genesis.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GenesisCommonConfig {
    private static final ForgeConfigSpec.IntValue ATMOSPHERE_EXIT_HEIGHT;
    private static final ForgeConfigSpec.IntValue ATMOSPHERE_ENTRY_HEIGHT;
    private static final ForgeConfigSpec.IntValue WORLD_SIZE;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ATMOSPHERE_EXIT_HEIGHT = builder
                .comment("Height above which anything in a celestial's dimension leaves for space")
                .defineInRange("atmosphereExitHeight", 840, -2048, 30_000_000);
        ATMOSPHERE_ENTRY_HEIGHT = builder
                .comment("Height at which anything arriving in a celestial's dimension from space is placed. Below the exit height, so it isn't sent straight back")
                .defineInRange("atmosphereEntryHeight", 720, -2048, 30_000_000);
        WORLD_SIZE = builder
                .comment("Width, in blocks, of the square around the origin of a celestial's dimension that its whole surface maps onto. Arriving from space lands within it, and leaving from outside it leaves from its nearest edge")
                .defineInRange("worldSize", 20_000, 8, 60_000_000);
        SPEC = builder.build();
    }

    private GenesisCommonConfig() {
    }

    public static int atmosphereExitHeight() {
        return ATMOSPHERE_EXIT_HEIGHT.get();
    }

    public static int atmosphereEntryHeight() {
        return ATMOSPHERE_ENTRY_HEIGHT.get();
    }

    public static int worldSize() {
        return WORLD_SIZE.get();
    }
}
