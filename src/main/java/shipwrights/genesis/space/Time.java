package shipwrights.genesis.space;

import net.minecraft.world.level.Level;

public record Time(long ticks, float partialTick) {

    public static Time get(Level level) {
        return get(level, 0f);
    }

    public static Time get(Level level, float partialTick) {
        // TODO: fix sleeping, time set commands, etc
        return new Time(level.getGameTime(), partialTick);
    }

    public double simple() {
        return ticks() + partialTick();
    }
}
