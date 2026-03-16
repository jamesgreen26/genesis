package shipwrights.genesis_ad_astra;

/**
 * Lightweight immutable 3D vector used for cube-map direction sampling.
 */
public final class Vec3 {
    public final double x, y, z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        double len = length();
        if (len < 1e-12) return new Vec3(0, 0, 1);
        return new Vec3(x / len, y / len, z / len);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.4f, %.4f, %.4f)", x, y, z);
    }
}
