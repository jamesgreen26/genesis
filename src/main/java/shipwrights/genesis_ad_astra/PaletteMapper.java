package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Maps each pixel in a generated texture to the nearest color in the extracted palette.
 * Optionally applies ordered dithering to reduce visible banding.
 */
public class PaletteMapper {

    // 4x4 Bayer matrix for ordered dithering (values 0–15)
    private static final int[][] BAYER4 = {
        { 0,  8,  2, 10},
        {12,  4, 14,  6},
        { 3, 11,  1,  9},
        {15,  7, 13,  5}
    };

    private final List<PaletteColor> palette;
    private final boolean dither;

    public PaletteMapper(List<PaletteColor> palette, boolean dither) {
        this.palette = palette;
        this.dither = dither;
    }

    public PaletteMapper(List<PaletteColor> palette) {
        this(palette, false);
    }

    /**
     * Remap all pixels in-place across all six cube faces.
     */
    public void remap(BufferedImage[] faces) {
        for (int f = 0; f < faces.length; f++) {
            remap(faces[f]);
        }
    }

    /**
     * Remap all pixels in a single face image.
     */
    public void remap(BufferedImage face) {
        int w = face.getWidth();
        int h = face.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = face.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (dither) {
                    int threshold = BAYER4[y & 3][x & 3]; // 0..15
                    int offset = (threshold - 7) * 4;     // spread: -28..+28
                    r = clamp(r + offset);
                    g = clamp(g + offset);
                    b = clamp(b + offset);
                }

                PaletteColor nearest = nearest(r, g, b);
                face.setRGB(x, y, nearest.toRGB());
            }
        }
    }

    /**
     * Find the nearest palette color for the given RGB.
     */
    public PaletteColor nearest(int r, int g, int b) {
        PaletteColor best = null;
        double bestDist = Double.MAX_VALUE;
        for (PaletteColor c : palette) {
            double d = c.distanceTo(r, g, b);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    /**
     * Interpolate between two palette colors by t ∈ [0,1], then snap.
     */
    public PaletteColor lerp(PaletteColor a, PaletteColor b, float t) {
        int r = clamp(Math.round(a.r + t * (b.r - a.r)));
        int g = clamp(Math.round(a.g + t * (b.g - a.g)));
        int bv = clamp(Math.round(a.b + t * (b.b - a.b)));
        return nearest(r, g, bv);
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
