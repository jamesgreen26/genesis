package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;

/**
 * Analyses a low-resolution input texture and returns a PatternDescriptor that
 * the generator selection step uses to pick the most appropriate noise model.
 */
public class PatternAnalyzer {

    public static class PatternDescriptor {
        public float rowVariance;       // high → horizontal stripes
        public float colVariance;       // high → vertical stripes
        public float horizontalScore;   // combined horizontal banding score
        public float verticalScore;     // combined vertical banding score
        public float isotropyScore;     // high → roughly isotropic noise / terrain
        public float blobScore;         // high → blobs / cellular features
        public float avgBlobSize;       // mean connected-component area
        public GeneratorType recommended;

        @Override
        public String toString() {
            return String.format(
                "PatternDescriptor{rowVar=%.3f, colVar=%.3f, hScore=%.3f, vScore=%.3f, " +
                "isotropy=%.3f, blob=%.3f, blobSize=%.1f, => %s}",
                rowVariance, colVariance, horizontalScore, verticalScore,
                isotropyScore, blobScore, avgBlobSize, recommended);
        }
    }

    /**
     * Analyse the given image and return a pattern descriptor.
     */
    public PatternDescriptor analyze(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        float[][] lum = luminance(image, w, h);

        PatternDescriptor desc = new PatternDescriptor();
        desc.rowVariance = rowVariance(lum, w, h);
        desc.colVariance = colVariance(lum, w, h);
        desc.horizontalScore = horizontalScore(lum, w, h);
        desc.verticalScore   = verticalScore(lum, w, h);
        desc.isotropyScore   = isotropyScore(desc);
        float[] blobResult   = blobScore(lum, w, h);
        desc.blobScore       = blobResult[0];
        desc.avgBlobSize     = blobResult[1];
        desc.recommended     = select(desc);
        return desc;
    }

    // -------------------------------------------------------------------------
    // Luminance helper
    // -------------------------------------------------------------------------

    private float[][] luminance(BufferedImage img, int w, int h) {
        float[][] lum = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                lum[y][x] = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
            }
        }
        return lum;
    }

    // -------------------------------------------------------------------------
    // Variance metrics
    // -------------------------------------------------------------------------

    /** Variance between row means → high for horizontal stripes */
    private float rowVariance(float[][] lum, int w, int h) {
        float[] rowMean = new float[h];
        for (int y = 0; y < h; y++) {
            float sum = 0;
            for (int x = 0; x < w; x++) sum += lum[y][x];
            rowMean[y] = sum / w;
        }
        return variance(rowMean);
    }

    /** Variance between column means → high for vertical stripes */
    private float colVariance(float[][] lum, int w, int h) {
        float[] colMean = new float[w];
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int y = 0; y < h; y++) sum += lum[y][x];
            colMean[x] = sum / h;
        }
        return variance(colMean);
    }

    private float variance(float[] arr) {
        float mean = 0;
        for (float v : arr) mean += v;
        mean /= arr.length;
        float var = 0;
        for (float v : arr) var += (v - mean) * (v - mean);
        return var / arr.length;
    }

    // -------------------------------------------------------------------------
    // Horizontal / vertical banding scores via autocorrelation of row means
    // -------------------------------------------------------------------------

    private float horizontalScore(float[][] lum, int w, int h) {
        // Measure how much row means vary periodically (autocorrelation at lag 1..h/2)
        float[] rowMean = new float[h];
        for (int y = 0; y < h; y++) {
            float s = 0;
            for (int x = 0; x < w; x++) s += lum[y][x];
            rowMean[y] = s / w;
        }
        return periodicityScore(rowMean);
    }

    private float verticalScore(float[][] lum, int w, int h) {
        float[] colMean = new float[w];
        for (int x = 0; x < w; x++) {
            float s = 0;
            for (int y = 0; y < h; y++) s += lum[y][x];
            colMean[x] = s / h;
        }
        return periodicityScore(colMean);
    }

    /** Simple normalized autocorrelation energy across lags 1..n/2 */
    private float periodicityScore(float[] sig) {
        int n = sig.length;
        float mean = 0;
        for (float v : sig) mean += v;
        mean /= n;
        float var = 0;
        for (float v : sig) var += (v - mean) * (v - mean);
        if (var < 1e-6f) return 0;

        float score = 0;
        int lags = Math.max(1, n / 2);
        for (int lag = 1; lag <= lags; lag++) {
            float cov = 0;
            for (int i = 0; i < n - lag; i++)
                cov += (sig[i] - mean) * (sig[i + lag] - mean);
            score += Math.abs(cov / ((n - lag) * var));
        }
        return score / lags;
    }

    // -------------------------------------------------------------------------
    // Isotropy score
    // -------------------------------------------------------------------------

    private float isotropyScore(PatternDescriptor d) {
        // High isotropy → neither dimension dominates
        float maxStripe = Math.max(d.horizontalScore, d.verticalScore);
        return 1f - Math.min(1f, maxStripe * 4f);
    }

    // -------------------------------------------------------------------------
    // Blob detection via connected components on thresholded luminance
    // -------------------------------------------------------------------------

    private float[] blobScore(float[][] lum, int w, int h) {
        // Threshold at mean luminance
        float mean = 0;
        for (float[] row : lum) for (float v : row) mean += v;
        mean /= (w * h);

        boolean[][] fg = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                fg[y][x] = lum[y][x] > mean;

        // BFS connected components (4-connected)
        int[] labels = new int[h * w];
        java.util.Arrays.fill(labels, -1);
        int nextLabel = 0;
        java.util.List<Integer> componentSizes = new java.util.ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!fg[y][x] || labels[y * w + x] >= 0) continue;
                // BFS
                int label = nextLabel++;
                java.util.Queue<int[]> queue = new java.util.LinkedList<>();
                queue.add(new int[]{x, y});
                labels[y * w + x] = label;
                int size = 0;
                while (!queue.isEmpty()) {
                    int[] cur = queue.poll();
                    size++;
                    int[] dx = {1,-1,0,0};
                    int[] dy = {0,0,1,-1};
                    for (int d = 0; d < 4; d++) {
                        int nx = cur[0] + dx[d], ny = cur[1] + dy[d];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h
                                && fg[ny][nx] && labels[ny * w + nx] < 0) {
                            labels[ny * w + nx] = label;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
                componentSizes.add(size);
            }
        }

        if (componentSizes.isEmpty()) return new float[]{0f, 0f};

        float avgSize = 0;
        for (int s : componentSizes) avgSize += s;
        avgSize /= componentSizes.size();

        int totalPixels = w * h;
        // blobScore: high when there are moderate-sized, distinct blobs
        float blobScore = Math.min(1f, (componentSizes.size() * avgSize) / (totalPixels * 0.5f));

        return new float[]{blobScore, avgSize};
    }

    // -------------------------------------------------------------------------
    // Generator selection
    // -------------------------------------------------------------------------

    private GeneratorType select(PatternDescriptor d) {
        float h = d.horizontalScore;
        float v = d.verticalScore;
        float b = d.blobScore;
        float i = d.isotropyScore;

        // Strong horizontal banding → stripes (gas giants)
        if (h > 0.15f && h > v * 1.5f) return GeneratorType.STRIPES;
        // Strong vertical banding
        if (v > 0.15f && v > h * 1.5f) return GeneratorType.STRIPES;
        // Clear blob structure → Worley
        if (b > 0.4f && i < 0.5f) return GeneratorType.WORLEY;
        // Mixed banding + noise
        if (h > 0.05f || v > 0.05f) return GeneratorType.HYBRID;
        // Default: fractal terrain noise
        return GeneratorType.FRACTAL;
    }
}
