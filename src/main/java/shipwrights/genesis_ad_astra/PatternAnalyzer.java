package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;

/**
 * Analyses a low-resolution input texture and returns a PatternDescriptor
 * describing its structure so the correct procedural generator can be chosen.
 *
 * Core insight (derived from empirical testing with real planet seed images):
 *
 *   A horizontally striped planet (e.g. Venus/gas giant) has:
 *     - HIGH row alternation  (row means oscillate high/low)
 *     - LOW column alternation (column means are smooth — stripes are consistent
 *                               across x, varying only in y)
 *
 *   A blob/terrain planet (e.g. Mercury, Phobos) has:
 *     - SIMILAR row and column alternation (variation is isotropic)
 *
 *   Stripe signal = rowAlt × (1 − colAlt)
 *   This reliably separates the two cases even on 8×8 inputs.
 *
 * All channel data (lum, R, B) is checked; the max across channels is used so
 * that low-luma-contrast colour stripes are still detected.
 */
public class PatternAnalyzer {

    // Stripe is confirmed when the directional stripe signal exceeds this value.
    // Empirically validated: non-stripe planets score <= 0.34, stripe planets >= 0.73.
    private static final float STRIPE_THRESHOLD = 0.60f;
    // Weak banding (hybrid) threshold.
    private static final float HYBRID_THRESHOLD = 0.50f;

    // =========================================================================
    // Descriptor
    // =========================================================================

    public static class PatternDescriptor {
        public float rowAltLum;      // row alternation score (luminance)
        public float colAltLum;      // col alternation score (luminance)
        public float rowAltColor;    // row alternation score (max of R, B)
        public float colAltColor;    // col alternation score (max of R, B)
        public float hStripeSignal;  // rowAlt * (1 - colAlt) — horizontal stripes
        public float vStripeSignal;  // colAlt * (1 - rowAlt) — vertical stripes
        public float isotropyScore;
        public float blobScore;
        public float avgBlobSize;
        public GeneratorType recommended;

        @Override
        public String toString() {
            return String.format(
                    "PatternDescriptor{\n" +
                            "  rowAltLum=%.3f colAltLum=%.3f rowAltColor=%.3f colAltColor=%.3f\n" +
                            "  hStripe=%.3f  vStripe=%.3f  isotropy=%.3f  blob=%.3f\n" +
                            "  => %s}",
                    rowAltLum, colAltLum, rowAltColor, colAltColor,
                    hStripeSignal, vStripeSignal, isotropyScore, blobScore,
                    recommended);
        }
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public PatternDescriptor analyze(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        float[][] lum   = luminance(image, w, h);
        float[][] chanR = channel(image, w, h, 0);
        float[][] chanB = channel(image, w, h, 2);

        float[] rowLum = rowMeans(lum,   w, h);
        float[] colLum = colMeans(lum,   w, h);

        // Compute stripe signal independently per channel, then take the max.
        // Critical: mixing rowAlt from one channel with colAlt from another
        // produces false positives on blob/terrain textures.
        float hStripe = 0, vStripe = 0;
        for (float[][] chan : new float[][][]{lum, chanR, chanB}) {
            float rA = alternationScore(rowMeans(chan, w, h));
            float cA = alternationScore(colMeans(chan, w, h));
            hStripe = Math.max(hStripe, rA * (1f - cA));
            vStripe = Math.max(vStripe, cA * (1f - rA));
        }

        PatternDescriptor desc = new PatternDescriptor();
        desc.rowAltLum   = alternationScore(rowLum);
        desc.colAltLum   = alternationScore(colLum);
        desc.rowAltColor = Math.max(alternationScore(rowMeans(chanR, w, h)),
                alternationScore(rowMeans(chanB, w, h)));
        desc.colAltColor = Math.max(alternationScore(colMeans(chanR, w, h)),
                alternationScore(colMeans(chanB, w, h)));
        desc.hStripeSignal = hStripe;
        desc.vStripeSignal = vStripe;
        desc.isotropyScore = isotropyScore(
                Math.max(desc.rowAltLum, desc.rowAltColor),
                Math.max(desc.colAltLum, desc.colAltColor));

        float[] blobResult = blobScore(lum, w, h);
        desc.blobScore   = blobResult[0];
        desc.avgBlobSize = blobResult[1];

        desc.recommended = select(desc);
        return desc;
    }

    // =========================================================================
    // Pixel data helpers
    // =========================================================================

    private float[][] luminance(BufferedImage img, int w, int h) {
        float[][] out = new float[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                out[y][x] = (0.2126f * ((rgb >> 16) & 0xFF)
                        + 0.7152f * ((rgb >>  8) & 0xFF)
                        + 0.0722f * ( rgb        & 0xFF)) / 255f;
            }
        return out;
    }

    private float[][] channel(BufferedImage img, int w, int h, int chan) {
        int shift = (chan == 0) ? 16 : (chan == 1) ? 8 : 0;
        float[][] out = new float[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out[y][x] = ((img.getRGB(x, y) >> shift) & 0xFF) / 255f;
        return out;
    }

    private float[] rowMeans(float[][] data, int w, int h) {
        float[] means = new float[h];
        for (int y = 0; y < h; y++) {
            float s = 0;
            for (int x = 0; x < w; x++) s += data[y][x];
            means[y] = s / w;
        }
        return means;
    }

    private float[] colMeans(float[][] data, int w, int h) {
        float[] means = new float[w];
        for (int x = 0; x < w; x++) {
            float s = 0;
            for (int y = 0; y < h; y++) s += data[y][x];
            means[x] = s / h;
        }
        return means;
    }

    // =========================================================================
    // Alternation score
    // =========================================================================

    /**
     * Measures how much a 1D signal alternates between high and low values.
     * Uses mean absolute second difference normalised by range.
     *
     *   ~1.0 = perfect square-wave oscillation (clear stripes)
     *   ~0.5 = noisy / blobby texture (isotropic variation)
     *   ~0.0 = flat or pure linear gradient
     *
     * This is the core metric: a striped axis has HIGH alternation, and the
     * perpendicular axis of a striped texture has LOW alternation (the stripes
     * look the same across their length).
     */
    private float alternationScore(float[] sig) {
        int n = sig.length;
        if (n < 3) return 0;
        float min = sig[0], max = sig[0];
        for (float v : sig) { if (v < min) min = v; if (v > max) max = v; }
        float range = max - min;
        if (range < 1e-4f) return 0;

        float sumAbsDiff2 = 0;
        for (int i = 1; i < n - 1; i++)
            sumAbsDiff2 += Math.abs(sig[i+1] - 2*sig[i] + sig[i-1]);
        return Math.min(1f, (sumAbsDiff2 / (n - 2)) / (range * 0.4f));
    }

    // =========================================================================
    // Isotropy score
    // =========================================================================

    /**
     * High isotropy means neither axis dominates — typical of fractal/blob terrain.
     */
    private float isotropyScore(float rowAlt, float colAlt) {
        float diff = Math.abs(rowAlt - colAlt);
        return 1f - Math.min(1f, diff * 2f);
    }

    // =========================================================================
    // Blob detection (connected components)
    // =========================================================================

    private float[] blobScore(float[][] lum, int w, int h) {
        float mean = 0;
        for (float[] row : lum) for (float v : row) mean += v;
        mean /= (w * h);

        boolean[][] fg = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                fg[y][x] = lum[y][x] > mean;

        int[] labels = new int[h * w];
        java.util.Arrays.fill(labels, -1);
        int nextLabel = 0;
        java.util.List<Integer> sizes = new java.util.ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!fg[y][x] || labels[y * w + x] >= 0) continue;
                int label = nextLabel++;
                java.util.Queue<int[]> q = new java.util.LinkedList<>();
                q.add(new int[]{x, y});
                labels[y * w + x] = label;
                int size = 0;
                while (!q.isEmpty()) {
                    int[] cur = q.poll();
                    size++;
                    int[] dx = {1, -1, 0,  0};
                    int[] dy = {0,  0, 1, -1};
                    for (int d = 0; d < 4; d++) {
                        int nx = cur[0] + dx[d], ny = cur[1] + dy[d];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h
                                && fg[ny][nx] && labels[ny * w + nx] < 0) {
                            labels[ny * w + nx] = label;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
                sizes.add(size);
            }
        }

        if (sizes.isEmpty()) return new float[]{0f, 0f};
        float avgSize = 0;
        for (int s : sizes) avgSize += s;
        avgSize /= sizes.size();
        float score = Math.min(1f, (sizes.size() * avgSize) / (w * h * 0.5f));
        return new float[]{score, avgSize};
    }

    // =========================================================================
    // Generator selection
    // =========================================================================

    private GeneratorType select(PatternDescriptor d) {
        float hSig = d.hStripeSignal;
        float vSig = d.vStripeSignal;

        if (hSig > STRIPE_THRESHOLD || vSig > STRIPE_THRESHOLD) return GeneratorType.STRIPES;
        if (hSig > HYBRID_THRESHOLD || vSig > HYBRID_THRESHOLD) return GeneratorType.HYBRID;
        if (d.blobScore > 0.4f && d.isotropyScore < 0.5f)       return GeneratorType.WORLEY;
        return GeneratorType.FRACTAL;
    }
}