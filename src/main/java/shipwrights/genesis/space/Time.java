package shipwrights.genesis.space;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public record Time(long ticks, float partialTick) {

    public static Time get(Level level) {
        return get(level, 0f);
    }

    /// The time in {@code level}, {@code partialTick} of the way to the next tick. Held at the tick itself while the
    /// daylight cycle's off, as the day time doesn't move on to the next, so nothing creeps between ticks towards it.
    /// On the client, the game rule's kept in step with the server's by the time it syncs.
    public static Time get(Level level, float partialTick) {
        boolean cycling = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        return of(level.dayTime(), cycling ? partialTick : 0f);
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
