package shipwrights.genesis.space;

import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalLong;

/// The time of day on a celestial, from where its light source actually is in its sky, as vanilla's time of day: 0 at
/// noon, 0.25 at sunset in the west, 0.5 at midnight and 0.75 at sunrise in the east. Vanilla works out sky brightness,
/// sky and sunset colours, daylight sensors, whether it's day and so on from that, so they follow the sun that's drawn
/// (see {@link shipwrights.genesis.mixin.LevelMixin}), rather than where vanilla's sun would be.
///
/// Like what's drawn, it's seen from where {@link VantagePoint} puts the observer on the celestial, the same for the
/// whole dimension.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class Daylight {

    /// How high vanilla's sun is when sleeping ends, at a time of day of about 0.785: a little above the horizon, as
    /// the cosine of its angle from straight up
    public static final double WAKING_SUN_HEIGHT = Math.cos(0.78453 * 2 * Math.PI);

    /// How far apart to look for the sun reaching somewhere, in ticks. Far less than a day, so it can't be stepped over
    private static final int SEARCH_STEP = 20;

    /// How far either side of the time asked for to look for the sun being where vanilla's would be then, in ticks:
    /// more than a day, so it's found at least once either side
    private static final int SET_TIME_SEARCH_RANGE = 26000;

    /// How far ahead to look for sunrise, in ticks
    private static final int SUNRISE_SEARCH_LIMIT = 10 * 24000;

    private Daylight() {
    }

    /// Where the light falling on a celestial at {@code position} comes from: itself if it casts light, otherwise the
    /// nearest celestial that does
    public static Optional<Vector3dc> nearestLightSource(Registry<Celestial> registry, Celestial celestial,
                                                         Vector3dc position, Time time) {
        if (celestial.castsLight()) return Optional.of(position);

        return registry.stream()
                .filter(Celestial::castsLight)
                .map(it -> it.transformProvider().getPosition(registry, time))
                .min(Comparator.comparingDouble(it -> it.distanceSquared(position)));
    }

    /// The time of day in {@code level} at {@code time}, if it's the dimension of a celestial, other than space, and
    /// its time isn't fixed
    public static Optional<Float> timeOfDay(Level level, Time time) {
        if (GenesisMod.isSpaceDimension(level) || level.dimensionType().hasFixedTime()) return Optional.empty();
        if (!(VantagePoint.get(level, time, new Vector3d()) instanceof VantagePoint.OnCelestial observer)) {
            return Optional.empty();
        }
        // Standing on a star, it's always noon
        if (observer.celestial().castsLight()) return Optional.of(0f);

        Vector3dc position = observer.getPosition();
        Optional<Vector3dc> light = nearestLightSource(observer.celestials(), observer.celestial(), position, time);
        // Without one, it's always night
        if (light.isEmpty()) return Optional.of(0.5f);

        Vector3d direction = new Vector3d(light.get()).sub(position)
                .rotate(observer.getRotation().conjugate(new Quaterniond()));
        return Optional.of(timeOfDay(direction));
    }

    /// The time of day with the sun in {@code direction} from the observer, in their frame (+X east, +Y up, +Z south).
    ///
    /// Vanilla's sun is at `(-sin θ, cos θ, 0)` for a time of day of `θ / 2π`, crossing the sky from east to west
    /// overhead. Taking the whole of the sun's direction across the horizon as east or west keeps how high it is, so how
    /// bright the sky is, wherever it really crosses the sky.
    public static float timeOfDay(Vector3dc direction) {
        double across = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        double angle = Math.atan2(direction.x() >= 0 ? -across : across, direction.y());
        double timeOfDay = angle / (2 * Math.PI);
        return (float) (timeOfDay - Math.floor(timeOfDay));
    }

    /// The first day time after {@code dayTime} when the sun climbs past how high vanilla's is when sleeping ends, if it
    /// does within {@value #SUNRISE_SEARCH_LIMIT} ticks. Whichever way it crosses the sky
    public static OptionalLong nextSunrise(Level level, long dayTime) {
        if (timeOfDay(level, Time.of(dayTime, 0f)).isEmpty()) return OptionalLong.empty();

        long previous = dayTime;
        for (long next = dayTime + SEARCH_STEP; next <= dayTime + SUNRISE_SEARCH_LIMIT; next += SEARCH_STEP) {
            if (risenBetween(level, previous, next)) {
                return OptionalLong.of(firstTick(previous, next, (from, to) -> risenBetween(level, from, to)));
            }
            previous = next;
        }
        return OptionalLong.empty();
    }

    /// The day time nearest {@code vanillaDayTime}, and not before 0, when the sun is where vanilla's would be at
    /// {@code vanillaDayTime}, rising or setting as it would be, as {@code /time set} sets. Vanilla's time where there's
    /// no sun of a celestial to follow, or it doesn't cross the sky the way vanilla's does, east to west
    public static long dayTimeFor(Level level, long vanillaDayTime) {
        if (timeOfDay(level, Time.of(vanillaDayTime, 0f)).isEmpty()) return vanillaDayTime;
        float target = level.dimensionType().timeOfDay(vanillaDayTime);

        long nearest = vanillaDayTime;
        long nearestDistance = Long.MAX_VALUE;
        long start = Math.max(0, vanillaDayTime - SET_TIME_SEARCH_RANGE);
        long previous = start;
        for (long next = start + SEARCH_STEP; next <= vanillaDayTime + SET_TIME_SEARCH_RANGE; next += SEARCH_STEP) {
            if (reachesBetween(level, previous, next, target)) {
                long tick = firstTick(previous, next, (from, to) -> reachesBetween(level, from, to, target));
                long distance = Math.abs(tick - vanillaDayTime);
                if (distance < nearestDistance) {
                    nearest = tick;
                    nearestDistance = distance;
                }
            }
            previous = next;
        }
        return nearest;
    }

    /// The time of day vanilla would have, from 0 up to 24000, with its sun where the sun is at {@code dayTime}, as
    /// {@code /time query daytime} reports. Vanilla's own where there's no sun of a celestial to follow
    public static int vanillaDayTime(Level level, long dayTime) {
        Optional<Float> timeOfDay = timeOfDay(level, Time.of(dayTime, 0f));
        if (timeOfDay.isEmpty()) return (int) (dayTime % 24000);

        // Vanilla's time of day only ever increases through the day, so search for where it's this one
        double low = 0;
        double high = 1;
        for (int i = 0; i < 40; i++) {
            double middle = (low + high) / 2;
            if (vanillaTimeOfDay(middle) < timeOfDay.get()) {
                low = middle;
            } else {
                high = middle;
            }
        }
        // Vanilla's day starts a quarter of a day before noon
        double day = (low + high) / 2 + 0.25;
        return (int) (Math.round((day - Math.floor(day)) * 24000) % 24000);
    }

    /// {@link net.minecraft.world.level.dimension.DimensionType#timeOfDay}, from how far through the day from noon it is
    private static double vanillaTimeOfDay(double sinceNoon) {
        return (sinceNoon * 2 + 0.5 - Math.cos(sinceNoon * Math.PI) / 2) / 3;
    }

    /// Whether the time of day passes {@code target} going forward between {@code from} and {@code to}
    private static boolean reachesBetween(Level level, long from, long to, float target) {
        return since(level, from, target) > 0.5 && since(level, to, target) < 0.5;
    }

    /// How far the time of day is past {@code target} at {@code dayTime}, from 0 up to 1
    private static double since(Level level, long dayTime, float target) {
        double since = timeOfDay(level, Time.of(dayTime, 0f)).orElse(0f) - target;
        return since - Math.floor(since);
    }

    /// The first tick after {@code from}, up to {@code to}, by which something has happened, given that it has by
    /// {@code to}: when {@code happenedBetween} first holds from {@code from}
    private static long firstTick(long from, long to, TickRange happenedBetween) {
        long low = from;
        long high = to;
        while (high - low > 1) {
            long middle = (low + high) / 2;
            if (happenedBetween.test(low, middle)) {
                high = middle;
            } else {
                low = middle;
            }
        }
        return high;
    }

    private interface TickRange {
        boolean test(long from, long to);
    }

    /// Whether the sun climbs past {@link #WAKING_SUN_HEIGHT} between {@code from} and {@code to}
    private static boolean risenBetween(Level level, long from, long to) {
        return sunHeight(level, from) < WAKING_SUN_HEIGHT && sunHeight(level, to) >= WAKING_SUN_HEIGHT;
    }

    /// How high the sun is at {@code dayTime}, as the cosine of its angle from straight up
    public static double sunHeight(Level level, long dayTime) {
        return Math.cos(timeOfDay(level, Time.of(dayTime, 0f)).orElse(0.5f) * 2 * Math.PI);
    }

    /// Sleeping through the night ends at the next sunrise, rather than at vanilla's next morning
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        nextSunrise(level, level.getDayTime()).ifPresent(event::setTimeAddition);
    }
}
