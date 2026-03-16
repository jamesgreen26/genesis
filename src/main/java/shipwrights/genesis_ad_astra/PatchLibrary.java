package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------------
// Patch data class
// ---------------------------------------------------------------------------

class Patch {
    public final int size;
    public final int[][] r;  // [row][col]
    public final int[][] g;
    public final int[][] b;
    public final float avgValue; // average luminance [0,1]

    public Patch(int size, int[][] r, int[][] g, int[][] b) {
        this.size = size;
        this.r = r;
        this.g = g;
        this.b = b;
        this.avgValue = computeAvg(r, g, b, size);
    }

    public int getR(int row, int col) { return r[row][col]; }
    public int getG(int row, int col) { return g[row][col]; }
    public int getB(int row, int col) { return b[row][col]; }

    private static float computeAvg(int[][] r, int[][] g, int[][] b, int size) {
        float sum = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                sum += (0.2126f * r[row][col] + 0.7152f * g[row][col]
                        + 0.0722f * b[row][col]) / 255f;
            }
        }
        return sum / (size * size);
    }
}

// ---------------------------------------------------------------------------
// PatchLibrary
// ---------------------------------------------------------------------------

/**
 * Extracts overlapping patches from the input texture and provides nearest-
 * match lookup by target luminance value.
 */
public class PatchLibrary {

    private static final int[] PATCH_SIZES = {3, 4, 5};

    private final List<Patch> patches = new ArrayList<>();

    /**
     * Build patch library from the given source image.
     */
    public PatchLibrary(BufferedImage source) {
        build(source);
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    private void build(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // Pre-read pixels
        int[][] pr = new int[h][w];
        int[][] pg = new int[h][w];
        int[][] pb = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                pr[y][x] = (rgb >> 16) & 0xFF;
                pg[y][x] = (rgb >> 8)  & 0xFF;
                pb[y][x] = rgb         & 0xFF;
            }
        }

        // Extract all valid patches for each size
        for (int sz : PATCH_SIZES) {
            for (int py = 0; py <= h - sz; py++) {
                for (int px = 0; px <= w - sz; px++) {
                    int[][] rp = new int[sz][sz];
                    int[][] gp = new int[sz][sz];
                    int[][] bp = new int[sz][sz];
                    for (int row = 0; row < sz; row++) {
                        for (int col = 0; col < sz; col++) {
                            rp[row][col] = pr[py + row][px + col];
                            gp[row][col] = pg[py + row][px + col];
                            bp[row][col] = pb[py + row][px + col];
                        }
                    }
                    patches.add(new Patch(sz, rp, gp, bp));
                }
            }
        }

        // If the input is very small (≤ 4 px) we might have too few patches;
        // replicate with mirrored/rotated variants to enlarge the library.
        if (patches.size() < 20) {
            augment();
        }
    }

    // -------------------------------------------------------------------------
    // Augmentation (mirror & rotate existing patches)
    // -------------------------------------------------------------------------

    private void augment() {
        List<Patch> originals = new ArrayList<>(patches);
        for (Patch p : originals) {
            patches.add(flipH(p));
            patches.add(flipV(p));
            patches.add(rotate90(p));
        }
    }

    private Patch flipH(Patch p) {
        int s = p.size;
        int[][] r = new int[s][s], g = new int[s][s], b = new int[s][s];
        for (int row = 0; row < s; row++)
            for (int col = 0; col < s; col++) {
                r[row][col] = p.r[row][s-1-col];
                g[row][col] = p.g[row][s-1-col];
                b[row][col] = p.b[row][s-1-col];
            }
        return new Patch(s, r, g, b);
    }

    private Patch flipV(Patch p) {
        int s = p.size;
        int[][] r = new int[s][s], g = new int[s][s], b = new int[s][s];
        for (int row = 0; row < s; row++)
            for (int col = 0; col < s; col++) {
                r[row][col] = p.r[s-1-row][col];
                g[row][col] = p.g[s-1-row][col];
                b[row][col] = p.b[s-1-row][col];
            }
        return new Patch(s, r, g, b);
    }

    private Patch rotate90(Patch p) {
        int s = p.size;
        int[][] r = new int[s][s], g = new int[s][s], b = new int[s][s];
        for (int row = 0; row < s; row++)
            for (int col = 0; col < s; col++) {
                r[row][col] = p.r[s-1-col][row];
                g[row][col] = p.g[s-1-col][row];
                b[row][col] = p.b[s-1-col][row];
            }
        return new Patch(s, r, g, b);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /**
     * Return the patch whose average luminance is closest to the target value.
     * targetValue should be in [0, 1].
     */
    public Patch match(double targetValue) {
        Patch best = null;
        double bestDist = Double.MAX_VALUE;
        for (Patch p : patches) {
            double d = Math.abs(p.avgValue - targetValue);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    public int size() { return patches.size(); }
}
