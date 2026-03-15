package shipwrights.genesis.teleportation.integration;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanetToSpaceTeleporterTest {

    private static final double EPS = 1e-9;

    // Matches the frame rotation used by computeSpaceTarget: celestialRotation * cameraRotation⁻¹.
    // With identity celestial rotation and cameraRotation = rotateTo(Y, -Z), the frame rotation
    // is rotateTo(Y, -Z).conjugate() = rotateTo(Y, +Z).
    // "Away from planet" = frameRotation.transform(0,1,0) = (0,0,+1).
    private static final Quaterniondc VANTAGE_ROTATION =
            new Quaterniond().rotateTo(new Vector3d(0, 1, 0), new Vector3d(0, 0, 1));

    @Test
    @DisplayName("Identity ship rotation: local up (0,1,0) should point away from planet")
    void identityShipRotation_localUpPointsAwayFromPlanet() {
        Quaterniondc shipRotation = new Quaterniond(); // identity

        Quaterniondc spaceRotation = PlanetToSpaceTeleporter.computeSpaceRotation(VANTAGE_ROTATION, shipRotation);

        // spaceRotation = frameRotation, so local-up → (0,0,+1)
        Vector3d result = spaceRotation.transform(0, 1, 0, new Vector3d());
        assertEquals(0.0, result.x, EPS, "X should be 0");
        assertEquals(0.0, result.y, EPS, "Y should be 0");
        assertEquals(1.0, result.z, EPS, "Z should be +1 (away from planet)");
    }

    @Test
    @DisplayName("Ship rotated so local north (0,0,+1) maps to world-up: local north should point away from planet")
    void northPointingUpShip_localNorthPointsAwayFromPlanet() {
        // Ship is rotated so local north (0,0,+1) points world-up (0,1,0)
        Quaterniondc shipRotation =
                new Quaterniond().rotateTo(new Vector3d(0, 0, 1), new Vector3d(0, 1, 0));

        Quaterniondc spaceRotation = PlanetToSpaceTeleporter.computeSpaceRotation(VANTAGE_ROTATION, shipRotation);

        // spaceRotation.transform(0,0,+1)
        //   = frameRotation.transform(shipRotation.transform(0,0,+1))
        //   = frameRotation.transform(0,1,0)
        //   = (0,0,+1) = "away from planet"
        Vector3d result = spaceRotation.transform(0, 0, 1, new Vector3d());
        assertEquals(0.0, result.x, EPS, "X should be 0");
        assertEquals(0.0, result.y, EPS, "Y should be 0");
        assertEquals(1.0, result.z, EPS, "Z should be +1 (away from planet)");
    }
}
