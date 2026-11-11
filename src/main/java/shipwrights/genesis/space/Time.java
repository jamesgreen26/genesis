package shipwrights.genesis.space;

import net.minecraft.world.level.Level;

public record Time(long ticks, float partialTick) {

    public static Time get(Level level) {
        return get(level, 0f);
    }

    public static Time get(Level level, float partialTick) {
        return of(level.dayTime(), partialTick);
    }

    /// The time at the level day time {@code dayTime}. Taken as it is, which puts the overworld's sun where vanilla's
    /// is: overhead at noon, 6000, setting at 12000 and so on, as its day is 24000 ticks long (see
    /// {@link Daylight} for celestials whose days aren't)
    public static Time of(long dayTime, float partialTick) {
        return new Time(dayTime, partialTick);
    }

    public double simple() {
        return ticks() + partialTick();
    }
}
