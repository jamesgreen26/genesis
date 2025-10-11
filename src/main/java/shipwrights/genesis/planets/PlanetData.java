package shipwrights.genesis.planets;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import java.util.Random;

public class PlanetData {
    public final ResourceLocation dimensionID;
    public final double sunDist;
    private final double theta;
    private final double phi;
    public Vector3d rot;
    public final int orbitalPeriod = 256 * 24000;
    public final double size;
    public final float color;

    public final int hash;

    public PlanetData(ResourceLocation dimensionID, double size, double sunDist, float r, float g, float b) {

        int hash = dimensionID.toString().hashCode();
        Random rand = new Random(hash);

        this.dimensionID = dimensionID;
        this.sunDist = sunDist;
        this.rot = new Vector3d(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
        this.size = size * GenesisMod.earthSize;
        this.color = rgbToFloat(r, g, b);
        this.hash = dimensionID.hashCode();

        for (int i = 0; i < rand.nextInt(2, 20); i++) {
            rand.nextDouble();
        }

        this.theta = rand.nextDouble() * 2 * Math.PI;   // longitude
        this.phi   = (Math.acos(2 * rand.nextDouble() - 1) + Math.PI) / 3; // latitude
    }

    public Vector3d getCurrentPos(long ticks, float subticks) {
        Vector3d out = new Vector3d(1, 0, 0);
        out = out.rotateY(Math.PI * 2 * (ticks + subticks) / orbitalPeriod);
        out = out.rotateY(theta);
        out = out.rotateX(phi + Math.PI / 2);
        return out.normalize(sunDist * GenesisMod.earthDist);
    }

    public Vector3d getCurrentPos(long ticks) {
        return getCurrentPos(ticks, 0f);
    }

    public static float rgbToFloat(float r, float g, float b) {
        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;

        int packed = (ri << 16) | (gi << 8) | bi;

        return Float.intBitsToFloat(packed);
    }

    public static int[] floatToRgb(float packedFloat) {
        int packed = Float.floatToIntBits(packedFloat);

        int r = ((packed >> 16) & 0xFF);
        int g = ((packed >> 8) & 0xFF);
        int b = (packed & 0xFF);

        return new int[] { r, g, b };
    }

    @Override
    public String toString() {
        return "PlanetData{" +
                "dimensionID=" + dimensionID +
                '}';
    }
}
