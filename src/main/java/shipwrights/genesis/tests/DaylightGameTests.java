package shipwrights.genesis.tests;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Daylight;
import shipwrights.genesis.space.Time;

import java.util.OptionalLong;

/// Daylight in the overworld, which is a celestial, follows its sun. Leaves the time as it is, as other tests depend on
/// where celestials are
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class DaylightGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    @GameTest(template = TEMPLATE)
    public static void timeOfDayFollowsDrawnSun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long dayTime = level.getDayTime();
        float expected = Daylight.timeOfDay(level, Time.of(dayTime, 0.5f)).orElseThrow();
        float actual = level.getTimeOfDay(0.5f);
        helper.assertTrue(Math.abs(expected - actual) < 1e-6, "time of day is " + actual + ", not " + expected);
        helper.assertTrue(Math.abs(level.getSunAngle(0.5f) - expected * 2 * Math.PI) < 1e-4, "sun angle doesn't follow");
        helper.succeed();
    }

    /// The overworld's day lines up with vanilla's, so its sun is where vanilla's is at the same day time
    @GameTest(template = TEMPLATE)
    public static void overworldDayLinesUpWithVanillas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / 24000 * 24000;
        for (long vanilla : new long[]{0, 6000, 12000, 18000}) {
            // The sun turns steadily: a quarter of the way round every 6000 ticks from overhead at 6000
            long dayTime = day + vanilla;
            double expected = (vanilla / 24000.0 - 0.25 + 1) % 1;
            float actual = Daylight.timeOfDay(level, Time.of(dayTime, 0f)).orElseThrow();
            double difference = Math.abs(expected - actual);
            helper.assertTrue(Math.min(difference, 1 - difference) < 0.002,
                    "at " + vanilla + " the sun is at " + actual + ", not " + expected);
        }
        // Vanilla's sun moves at its own pace through the day, but meets it at noon and midnight, where /time set
        // leaves the time as it is
        for (long vanilla : new long[]{6000, 18000}) {
            long dayTime = day + vanilla;
            long set = Daylight.dayTimeFor(level, dayTime);
            helper.assertTrue(Math.abs(set - dayTime) < 60, "/time set " + dayTime + " sets " + set);
        }
        helper.succeed();
    }

    /// What {@code /time set} and {@code /time query daytime} use
    @GameTest(template = TEMPLATE)
    public static void vanillaTimesPutSunWhereVanillasWouldBe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (long vanilla : new long[]{1000, 6000, 13000, 18000, 24000 * 3 + 1000}) {
            long dayTime = Daylight.dayTimeFor(level, vanilla);
            helper.assertTrue(dayTime >= 0 && Math.abs(dayTime - vanilla) <= 26000,
                    "/time set " + vanilla + " sets " + dayTime);

            // Where vanilla's sun would be, as high and on the same side
            float expected = level.dimensionType().timeOfDay(vanilla);
            float actual = Daylight.timeOfDay(level, Time.of(dayTime, 0f)).orElseThrow();
            double difference = Math.abs(expected - actual);
            helper.assertTrue(Math.min(difference, 1 - difference) < 0.001,
                    "/time set " + vanilla + " puts the sun at " + actual + ", not " + expected);

            int reported = Daylight.vanillaDayTime(level, dayTime);
            long reportedDifference = Math.abs(reported - vanilla % 24000);
            helper.assertTrue(Math.min(reportedDifference, 24000 - reportedDifference) <= 20,
                    "/time set " + vanilla + " reports " + reported);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nextSunriseIsWhenSunRises(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long now = level.getDayTime();
        OptionalLong found = Daylight.nextSunrise(level, now);
        helper.assertTrue(found.isPresent(), "no sunrise found");
        long sunrise = found.getAsLong();

        // The overworld turns once every 24000 ticks, and goes round the sun far slower
        helper.assertTrue(sunrise > now && sunrise <= now + 24500, "next sunrise is " + (sunrise - now) + " ticks away");
        double before = Daylight.sunHeight(level, sunrise - 1);
        double at = Daylight.sunHeight(level, sunrise);
        helper.assertTrue(before < Daylight.WAKING_SUN_HEIGHT, "sun is already up a tick before sunrise: " + before);
        helper.assertTrue(at >= Daylight.WAKING_SUN_HEIGHT && at < Daylight.WAKING_SUN_HEIGHT + 0.01,
                "sun isn't just up at sunrise: " + at);
        // Rising, not setting
        helper.assertTrue(Daylight.sunHeight(level, sunrise + 200) > at, "sun is setting at sunrise");
        // In the east, as vanilla's does, so the time of day runs forwards
        float timeOfDay = Daylight.timeOfDay(level, Time.of(sunrise, 0f)).orElseThrow();
        helper.assertTrue(timeOfDay > 0.5f, "sun rises in the west: time of day " + timeOfDay);
        helper.succeed();
    }
}
