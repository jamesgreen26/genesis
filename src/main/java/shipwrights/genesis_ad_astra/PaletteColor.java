package shipwrights.genesis_ad_astra;

public class PaletteColor {
    public int r, g, b;
    public float weight;

    public PaletteColor(int r, int g, int b, float weight) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.weight = weight;
    }

    public int toRGB() {
        return (r << 16) | (g << 8) | b;
    }

    public double distanceTo(int pr, int pg, int pb) {
        double dr = r - pr;
        double dg = g - pg;
        double db = b - pb;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    @Override
    public String toString() {
        return String.format("PaletteColor(%d, %d, %d, w=%.3f)", r, g, b, weight);
    }
}