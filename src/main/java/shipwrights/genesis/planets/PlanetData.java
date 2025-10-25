package shipwrights.genesis.planets;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.data.SystemConfigModel;

import java.util.Random;

public class PlanetData {
    @Nullable public final PlanetData parent;
    public final ResourceLocation dimensionID;
    public final double orbitRadius;
    private final double orbitalTheta;
    private final double orbitalPhi;
    private final double orbitalPeriod;
    public Vector3d rotation;
    private final double size;
    public final float color;
    public final double gravity;

    public final int hash;

    public PlanetData(ResourceLocation dimensionID, @Nullable PlanetData parent, double size, double orbitRadius, double yearLength, float r, float g, float b, double gravity) {

        int hash = dimensionID.toString().hashCode();
        Random rand = new Random(hash);

        this.parent = parent;
        this.dimensionID = dimensionID;
        this.orbitRadius = orbitRadius;
        this.rotation = new Vector3d(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
        this.size = size;
        this.orbitalPeriod = yearLength;
        this.color = rgbToFloat(r, g, b);
        this.gravity = gravity;
        this.hash = dimensionID.hashCode();

        for (int i = 0; i < rand.nextInt(2, 20); i++) {
            rand.nextDouble();
        }

        this.orbitalTheta = rand.nextDouble() * 2 * Math.PI;   // longitude
        this.orbitalPhi   = (Math.acos(2 * rand.nextDouble() - 1) + Math.PI) / 3; // latitude
    }

    public Vector3d getCurrentPos(long ticks, float subticks) {
        if (getYearLengthTicks() == 0 || orbitRadius == 0) {
            if (parent != null) {
                return parent.getCurrentPos(ticks, subticks);
            }
            return new Vector3d(0, 0, 0);
        }

        Vector3d out = new Vector3d(1, 0, 0);
        out = out.rotateY(Math.PI * 2 * (ticks + subticks) / getYearLengthTicks());
        out = out.rotateY(orbitalTheta);
        out = out.rotateX(orbitalPhi + Math.PI / 2);
        out.normalize(orbitRadius * GenesisMod.earthDist);
        if (parent != null) {
            out = out.add(parent.getCurrentPos(ticks, subticks), new Vector3d());
        }
        return out;
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

    public int getYearLengthTicks() {
        return (int)(this.orbitalPeriod * GenesisMod.earthYear);
    }

    public double getActualSize() {
         return this.size * GenesisMod.earthSize;
    }

    public SystemConfigModel.PlanetJsonModel toJsonModel() {
        int[] color = floatToRgb(this.color);

        return new SystemConfigModel.PlanetJsonModel(dimensionID.toString(), size, orbitRadius, orbitalPeriod, gravity, color[0], color[1], color[2]);
    }
}
