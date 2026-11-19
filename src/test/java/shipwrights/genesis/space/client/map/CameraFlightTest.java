package shipwrights.genesis.space.client.map;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraFlightTest {
    private static final Vector3d ORIGIN = new Vector3d();

    @Test
    void startsWhereItIsAndEndsExactlyAtTheTarget() {
        Vector3d target = new Vector3d(12_345, -6, 789);
        CameraFlight flight = new CameraFlight(ORIGIN, 1000, target, 50, 0);

        CameraFlight.View first = flight.at(0);
        assertFalse(first.done());
        assertEquals(0, first.center().distance(ORIGIN), 1e-6);
        assertEquals(1000, first.width(), 1e-6);

        CameraFlight.View last = flight.at(60_000);
        assertTrue(last.done());
        assertEquals(target, last.center());
        assertEquals(50, last.width());
    }

    @Test
    void zoomsOutPartwayToFlyFarAcross() {
        CameraFlight flight = new CameraFlight(ORIGIN, 100, new Vector3d(1e6, 0, 0), 100, 0);
        double widest = 0;
        for (long ms = 0; ms <= 2000; ms += 10) widest = Math.max(widest, flight.at(ms).width());
        double found = widest;
        assertTrue(found > 10_000, () -> "widest " + found);
    }

    @Test
    void movesSteadilyOnwardWithoutOvershooting() {
        Vector3d target = new Vector3d(0, 0, 5e5);
        CameraFlight flight = new CameraFlight(ORIGIN, 300, target, 3, 0);
        double last = 0;
        for (long ms = 0; ms <= 2000; ms += 10) {
            CameraFlight.View view = flight.at(ms);
            double along = view.center().z;
            assertTrue(along >= last - 1e-6 && along <= target.z + 1e-6, () -> "at " + along);
            assertTrue(Double.isFinite(view.width()) && view.width() > 0);
            last = along;
        }
        assertTrue(flight.at(2000).done());
    }

    @Test
    void onTheSpotItOnlyZooms() {
        CameraFlight flight = new CameraFlight(ORIGIN, 10, ORIGIN, 1000, 0);
        double last = 10;
        for (long ms = 0; ms <= 2000; ms += 10) {
            CameraFlight.View view = flight.at(ms);
            assertEquals(0, view.center().length(), 1e-9);
            assertTrue(view.width() >= last - 1e-9);
            last = view.width();
        }
        assertEquals(1000, last);
    }

    @Test
    void takesAsLongHoweverFarItGoes() {
        CameraFlight near = new CameraFlight(ORIGIN, 100, new Vector3d(10, 0, 0), 90, 0);
        CameraFlight far = new CameraFlight(ORIGIN, 100, new Vector3d(1e8, 0, 0), 0.5, 0);
        for (CameraFlight flight : new CameraFlight[]{near, far}) {
            assertFalse(flight.at(749).done());
            assertTrue(flight.at(750).done());
        }
    }

    @Test
    void goingNowhereStaysPutForAsLong() {
        CameraFlight flight = new CameraFlight(ORIGIN, 10, ORIGIN, 10, 0);
        CameraFlight.View partway = flight.at(375);
        assertFalse(partway.done());
        assertEquals(0.5, partway.progress(), 1e-9);
        assertEquals(0, partway.center().length(), 1e-9);
        assertEquals(10, partway.width(), 1e-9);
        assertTrue(flight.at(750).done());
    }
}
