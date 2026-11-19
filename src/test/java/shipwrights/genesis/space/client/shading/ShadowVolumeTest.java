package shipwrights.genesis.space.client.shading;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.space.math.OBB;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowVolumeTest {

    private static final double EPS = 1e-6;

    private static OBB unitCube(Vector3dc center) {
        return OBB.createCube(1.0, new Quaterniond(), center);
    }

    @Test
    void compute_overheadOccluder() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        OBB occluder = unitCube(new Vector3d(0, 0, 2));
        Vector3d light = new Vector3d(0, 0, 5);

        List<ShadowVolume> volumes = ShadowVolume.compute(self, List.of(occluder), light);

        assertEquals(1, volumes.size());
        ShadowVolume volume = volumes.get(0);
        // Seen face on, the occluder's silhouette is a square: its four sides, and the plane across its middle
        assertEquals(5, volume.planes().size());

        // The occluder's near face is 0.5 across at 2.5 from the light, so its shadow is 0.9 across at the top of
        // the cube, 4.5 from the light: wider than the cube
        assertTrue(volume.depth(new Vector3d(0, 0, 0.5)) >= volume.edgeWidth(),
                "The middle of the shadow should be at full strength");
        assertTrue(volume.depth(new Vector3d(0.5, 0.5, 0.5)) > 0, "The cube's corner is in shadow");
        assertTrue(volume.depth(new Vector3d(2, 0, 0.5)) < 0, "Beside the shadow");
        assertEquals(0, volume.depth(new Vector3d(0.9, 0, 0.5)), EPS, "On the shadow's edge");
    }

    @Test
    void compute_depthIsMeasuredInTheShadowedCubesLocalSpace() {
        Quaterniond orientation = new Quaterniond().rotateZ(Math.toRadians(90));
        OBB self = OBB.createCube(1.0, orientation, new Vector3d(10, 20, 30));
        OBB occluder = unitCube(new Vector3d(10, 20, 32));
        Vector3d light = new Vector3d(10, 20, 35);

        ShadowVolume volume = ShadowVolume.compute(self, List.of(occluder), light).get(0);

        // The shadow's edge at world (10.9, 20, 30.5) is at local (0, -0.9, 0.5)
        assertEquals(0, volume.depth(new Vector3d(0, -0.9, 0.5)), EPS);
        assertTrue(volume.depth(new Vector3d(0, -0.5, 0.5)) > 0);
        assertTrue(volume.depth(new Vector3d(0, -1.5, 0.5)) < 0);
    }

    @Test
    void compute_nothingForOccludersThatCantShadeTheCube() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        Vector3d light = new Vector3d(0, 0, 5);

        assertTrue(ShadowVolume.compute(self, List.of(unitCube(new Vector3d(0, 0, -2))), light).isEmpty(),
                "Behind the cube");
        assertTrue(ShadowVolume.compute(self, List.of(unitCube(new Vector3d(0, 0, 7))), light).isEmpty(),
                "Behind the light");
        assertTrue(ShadowVolume.compute(self, List.of(unitCube(new Vector3d(20, 0, 2))), light).isEmpty(),
                "Off to the side");
    }

    /// The moon coming out of the Overworld's shadow, with the Overworld turned corner on so that its shadow is at its
    /// widest. An occluder larger than what it shades must not be dropped while its shadow is still on it.
    @Test
    void compute_keepsLargeOccluderWhileItsShadowIsStillOnTheCube() {
        Vector3d light = new Vector3d(0, 0, 0);
        OBB overworld = OBB.createCube(768, new Quaterniond().rotateY(Math.toRadians(45)), new Vector3d(120000, 0, 0));
        // 630 to the side of the middle of the shadow, which reaches about 570 to that side
        double angle = 0.005;
        OBB moon = OBB.createCube(232, new Quaterniond(),
                new Vector3d(126000 * Math.cos(angle), 0, 126000 * Math.sin(angle)));

        List<ShadowVolume> volumes = ShadowVolume.compute(moon, List.of(overworld), light);

        assertEquals(1, volumes.size(), "The Overworld still shades the moon");
        assertTrue(volumes.get(0).depth(new Vector3d(0, 0, -116)) > 0, "The moon's near side is in shadow");
        assertTrue(volumes.get(0).depth(new Vector3d(0, 0, 116)) < 0, "The moon's far side is out of it");
    }

    @Test
    void compute_oneVolumePerOccluder() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        // Side by side with a gap between them, each shading a square 0.2 across
        OBB left = OBB.createCube(0.2, new Quaterniond(), new Vector3d(-0.2, 0, 1));
        OBB right = OBB.createCube(0.2, new Quaterniond(), new Vector3d(0.2, 0, 1));
        Vector3d light = new Vector3d(0, 0, 1000);

        List<ShadowVolume> volumes = ShadowVolume.compute(self, List.of(left, right), light);

        assertEquals(2, volumes.size());
        assertTrue(volumes.get(0).depth(new Vector3d(-0.2, 0, 0.5)) > 0, "Under the left occluder");
        assertTrue(volumes.get(1).depth(new Vector3d(0.2, 0, 0.5)) > 0, "Under the right occluder");
        Vector3d inGap = new Vector3d(0, 0, 0.5);
        assertTrue(volumes.stream().allMatch(volume -> volume.depth(inGap) < 0), "The gap between them is lit");
    }

    @Test
    void cast_onlyShadesWhatIsBehindTheOccluder() {
        // A flat ring around the occluder itself, as a celestial's ring is around the celestial
        OBB occluder = unitCube(new Vector3d(0, 0, 0));
        OBB ring = new OBB(new AABBd(-2, 0, -2, 2, 0, 2), new Quaterniond(), new Vector3d(0, 0, 0));
        Vector3d light = new Vector3d(0, 0, 1000);

        // Not found among what could shade the ring, being no nearer the light than the ring is
        assertTrue(ShadowVolume.compute(ring, List.of(occluder), light).isEmpty());

        ShadowVolume volume = ShadowVolume.cast(ring, occluder, light);

        assertNotNull(volume);
        assertTrue(volume.depth(new Vector3d(0, 0, -1.5)) >= volume.edgeWidth(), "Behind the occluder, in full shadow");
        assertTrue(volume.depth(new Vector3d(0, 0, 1.5)) < 0, "Between the light and the occluder");
        assertTrue(volume.depth(new Vector3d(1.5, 0, -1.5)) < 0, "Behind the occluder, but off to the side");
        assertTrue(volume.depth(new Vector3d(1.5, 0, 0)) < 0, "Beside the occluder");
    }

    @Test
    void cast_neverMorePlanesThanTheShaderHoldsRoomFor() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        Vector3d light = new Vector3d(300, 1000, 500);
        // Seen corner on, a cube's silhouette has the most edges it can: six
        OBB occluder = OBB.createCube(1.0, new Quaterniond().rotateXYZ(0.4, 0.7, 0.2), new Vector3d(3, 10, 5));

        ShadowVolume volume = ShadowVolume.cast(self, occluder, light);

        assertNotNull(volume);
        assertEquals(ShadowVolume.MAX_PLANES, volume.planes().size());
    }
}
