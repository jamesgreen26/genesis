package shipwrights.genesis.space;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaylightTest {

    private static final double EPSILON = 1e-5;

    @Test
    void cardinalDirections() {
        assertEquals(0, Daylight.timeOfDay(new Vector3d(0, 1, 0)), EPSILON);
        assertEquals(0.25, Daylight.timeOfDay(new Vector3d(-1, 0, 0)), EPSILON);
        assertEquals(0.5, Daylight.timeOfDay(new Vector3d(0, -1, 0)), EPSILON);
        assertEquals(0.75, Daylight.timeOfDay(new Vector3d(1, 0, 0)), EPSILON);
    }

    @Test
    void matchesVanillaSunAllTheWayRound() {
        for (int i = 0; i < 360; i++) {
            double timeOfDay = i / 360.0;
            double angle = timeOfDay * 2 * Math.PI;
            // Where vanilla draws its sun for that time of day
            Vector3d sun = new Vector3d(-Math.sin(angle), Math.cos(angle), 0);
            double actual = Daylight.timeOfDay(sun);
            // 0 and 1 are the same time
            double difference = Math.abs(actual - timeOfDay);
            assertEquals(0, Math.min(difference, 1 - difference), EPSILON, "at " + timeOfDay);
        }
    }

    @Test
    void keepsHeightOfTiltedSun() {
        for (double south = -1; south <= 1; south += 0.25) {
            for (double up = -1; up <= 1; up += 0.25) {
                for (double east : new double[]{-0.5, 0.5}) {
                    Vector3d sun = new Vector3d(east, up, south).normalize();
                    double angle = Daylight.timeOfDay(sun) * 2 * Math.PI;
                    // Vanilla's sun is cos(angle) high
                    assertEquals(sun.y, Math.cos(angle), EPSILON, "for " + sun);
                    // And on the same side, east or west
                    assertEquals(Math.signum(sun.x), Math.signum(-Math.sin(angle)), "for " + sun);
                }
            }
        }
    }

    @Test
    void ignoresDistance() {
        assertEquals(Daylight.timeOfDay(new Vector3d(1, 2, 3)), Daylight.timeOfDay(new Vector3d(1000, 2000, 3000)), EPSILON);
    }
}
