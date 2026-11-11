package shipwrights.genesis.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.genesis.space.Daylight;
import shipwrights.genesis.space.Time;

/// Takes the time of day from where the sun is drawn, on a celestial (see {@link Daylight}), which vanilla works out
/// daylight, sky colour, daylight sensors, whether it's day and so on from. Vanilla's comes from a default method of
/// {@link LevelTimeAccess} that {@link Level} doesn't override, so this adds the override, soft implemented so it's
/// renamed to match it in production.
@Mixin(Level.class)
@Implements(@Interface(iface = LevelTimeAccess.class, prefix = "genesis$"))
public abstract class LevelMixin {

    // It's asked for many times a tick and a frame, and finding the sun walks the chain of orbits, so keep the last
    @Unique
    private long genesis$cachedDayTime = Long.MIN_VALUE;
    @Unique
    private float genesis$cachedPartialTick;
    @Unique
    private float genesis$cachedTimeOfDay;

    public float genesis$getTimeOfDay(float partialTick) {
        Level level = (Level) (Object) this;
        long dayTime = level.getDayTime();
        if (dayTime != genesis$cachedDayTime || partialTick != genesis$cachedPartialTick) {
            genesis$cachedTimeOfDay = Daylight.timeOfDay(level, Time.of(dayTime, partialTick))
                    .orElseGet(() -> level.dimensionType().timeOfDay(dayTime));
            genesis$cachedDayTime = dayTime;
            genesis$cachedPartialTick = partialTick;
        }
        return genesis$cachedTimeOfDay;
    }
}
