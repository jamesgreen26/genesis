package shipwrights.genesis.planets;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;

import java.util.Random;

public class PlanetData {
    public final ResourceLocation dimensionID;
    public final Vector3d pos;
    public final Vector3d rot;
    public final double size;
    public final float color;

    public PlanetData(ResourceLocation dimensionID, double size, double sunDist, int r, int g, int b) {

        int hash = dimensionID.toString().hashCode();
        Random rand = new Random(hash);

        this.dimensionID = dimensionID;
        this.pos = randomPos(rand, sunDist);
        this.rot = new Vector3d(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
        this.size = size * GenesisMod.earthSize;
        this.color = rgbToFloat(r, g, b);
    }

    private Vector3d randomPos(Random rand, double sunDist) {

        double theta = rand.nextDouble() * 2 * Math.PI;   // longitude
        double phi   = Math.acos(2 * rand.nextDouble() - 1); // latitude (uniform sphere)
        double x = Math.sin(phi) * Math.cos(theta);
        double y = Math.sin(phi) * Math.sin(theta);
        double z = Math.cos(phi);
        return new Vector3d(x, y, z).normalize(sunDist * GenesisMod.earthDist);
    }

    public static float rgbToFloat(float r, float g, float b) {
        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;

        int packed = (ri << 16) | (gi << 8) | bi;

        return Float.intBitsToFloat(packed);
    }

    public static float[] floatToRgb(float packedFloat) {
        int packed = Float.floatToIntBits(packedFloat);

        float r = ((packed >> 16) & 0xFF) / 255.0f;
        float g = ((packed >> 8) & 0xFF) / 255.0f;
        float b = (packed & 0xFF) / 255.0f;

        return new float[] { r, g, b };
    }

    @Override
    public String toString() {
        return "PlanetData{" +
                "pos=" + pos.toString() +
                ", dimensionID=" + dimensionID +
                '}';
    }
}
