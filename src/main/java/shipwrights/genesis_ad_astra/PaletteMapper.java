package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Maps each pixel in a generated texture to the nearest color in the extracted palette.
 *
 * Dithering mode uses an 8×8 Bayer ordered dither matrix scaled to BLOCK_SIZE so the
 * dither pattern aligns with the pixel blocks produced by TextureSynthesizer.
 * This creates the characteristic checkerboard boundary transitions seen in
 * pixel-art planet textures.
 */
public class PaletteMapper {

    // 8×8 Bayer matrix (values 0–63), gives finer dither gradations than 4×4
    private static final int[][] BAYER8 = {
            { 0, 32,  8, 40,  2, 34, 10, 42},
            {48, 16, 56, 24, 50, 18, 58, 26},
            {12, 44,  4, 36, 14, 46,  6, 38},
            {60, 28, 52, 20, 62, 30, 54, 22},
            { 3, 35, 11, 43,  1, 33,  9, 41},
            {51, 19, 59, 27, 49, 17, 57, 25},
            {15, 47,  7, 39, 13, 45,  5, 37},
            {63, 31, 55, 23, 61, 29, 53, 21}
    };

    /**
     * How many output pixels correspond to one dither matrix cell.
     * Matches BLOCK_SIZE in TextureSynthesizer so dither boundaries align with
     * colour block boundaries — this is what gives the pixel-art checkerboard look.
     */
    private static final int DITHER_BLOCK = 1;

    /**
     * Dither spread: maximum ±offset applied to each channel before palette snap.
     * Larger = more colour transitions visible at boundaries.
     * Should be roughly half the distance between adjacent palette colours in RGB space.
     */
    private static final int DITHER_SPREAD = 4;

    private final List<PaletteColor> palette;
    private final boolean dither;

    public PaletteMapper(List<PaletteColor> palette, boolean dither) {
        this.palette = palette;
        this.dither = dither;
    }

    public PaletteMapper(List<PaletteColor> palette) {
        this(palette, false);
    }

    public void remap(BufferedImage[] faces) {
        for (BufferedImage face : faces) remap(face);
    }

    public void remap(BufferedImage face) {
        int w = face.getWidth();
        int h = face.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = face.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;

                if (dither) {
                    // Index into Bayer matrix using block-scaled coordinates.
                    // Dividing by DITHER_BLOCK means each matrix cell covers a
                    // DITHER_BLOCK×DITHER_BLOCK region of pixels, so the dither
                    // pattern appears as visible pixel blocks rather than single dots.
                    int bx = (x / DITHER_BLOCK) & 7;
                    int by = (y / DITHER_BLOCK) & 7;
                    int threshold = BAYER8[by][bx]; // 0..63

                    // Remap threshold to a signed offset: centre at 31.5 → ±DITHER_SPREAD
                    int offset = (int)((threshold - 31.5) / 63.0 * DITHER_SPREAD * 2);

                    r = clamp(r + offset);
                    g = clamp(g + offset);
                    b = clamp(b + offset);
                }

                PaletteColor nearest = nearest(r, g, b);
                face.setRGB(x, y, nearest.toRGB());
            }
        }
    }

    public PaletteColor nearest(int r, int g, int b) {
        PaletteColor best = null;
        double bestDist = Double.MAX_VALUE;
        for (PaletteColor c : palette) {
            double d = c.distanceTo(r, g, b);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    public PaletteColor lerp(PaletteColor a, PaletteColor b, float t) {
        int r  = clamp(Math.round(a.r + t * (b.r - a.r)));
        int g  = clamp(Math.round(a.g + t * (b.g - a.g)));
        int bv = clamp(Math.round(a.b + t * (b.b - a.b)));
        return nearest(r, g, bv);
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
