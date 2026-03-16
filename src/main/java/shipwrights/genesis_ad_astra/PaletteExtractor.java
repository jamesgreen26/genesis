package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Extracts a dominant color palette from the input texture using k-means clustering.
 */
public class PaletteExtractor {

    private static final int DEFAULT_PALETTE_SIZE = 16;
    private static final int MAX_ITERATIONS = 50;
    private static final Random RNG = new Random(42);

    /**
     * Extract a palette of up to paletteSize colors from the given image.
     */
    public List<PaletteColor> extract(BufferedImage image, int paletteSize) {
        int w = image.getWidth();
        int h = image.getHeight();

        // Collect all pixels
        List<int[]> pixels = new ArrayList<>(w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels.add(new int[]{r, g, b});
            }
        }

        // Clamp palette size to number of unique pixels
        paletteSize = Math.min(paletteSize, pixels.size());

        // K-means clustering
        int[][] centroids = initCentroids(pixels, paletteSize);
        int[] assignments = new int[pixels.size()];

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            boolean changed = assignPixels(pixels, centroids, assignments);
            updateCentroids(pixels, centroids, assignments, paletteSize);
            if (!changed) break;
        }

        // Count cluster populations
        int[] counts = new int[paletteSize];
        for (int a : assignments) counts[a]++;
        int total = pixels.size();

        // Build palette
        List<PaletteColor> palette = new ArrayList<>();
        for (int k = 0; k < paletteSize; k++) {
            if (counts[k] == 0) continue;
            float weight = (float) counts[k] / total;
            palette.add(new PaletteColor(centroids[k][0], centroids[k][1], centroids[k][2], weight));
        }

        // Sort by weight descending
        palette.sort((a, b2) -> Float.compare(b2.weight, a.weight));
        return palette;
    }

    public List<PaletteColor> extract(BufferedImage image) {
        return extract(image, DEFAULT_PALETTE_SIZE);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int[][] initCentroids(List<int[]> pixels, int k) {
        // K-means++ initialization
        int[][] centroids = new int[k][3];
        // Pick first centroid randomly
        int[] first = pixels.get(RNG.nextInt(pixels.size()));
        centroids[0] = Arrays.copyOf(first, 3);

        for (int c = 1; c < k; c++) {
            // For each pixel, find distance to nearest existing centroid
            double[] dist = new double[pixels.size()];
            double total = 0;
            for (int i = 0; i < pixels.size(); i++) {
                dist[i] = minDistSq(pixels.get(i), centroids, c);
                total += dist[i];
            }
            // Sample proportional to distance squared
            double r = RNG.nextDouble() * total;
            double cumul = 0;
            int chosen = 0;
            for (int i = 0; i < pixels.size(); i++) {
                cumul += dist[i];
                if (cumul >= r) { chosen = i; break; }
            }
            centroids[c] = Arrays.copyOf(pixels.get(chosen), 3);
        }
        return centroids;
    }

    private double minDistSq(int[] pixel, int[][] centroids, int count) {
        double min = Double.MAX_VALUE;
        for (int c = 0; c < count; c++) {
            double dr = pixel[0] - centroids[c][0];
            double dg = pixel[1] - centroids[c][1];
            double db = pixel[2] - centroids[c][2];
            double d = dr * dr + dg * dg + db * db;
            if (d < min) min = d;
        }
        return min;
    }

    private boolean assignPixels(List<int[]> pixels, int[][] centroids, int[] assignments) {
        boolean changed = false;
        for (int i = 0; i < pixels.size(); i++) {
            int[] p = pixels.get(i);
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int k = 0; k < centroids.length; k++) {
                double dr = p[0] - centroids[k][0];
                double dg = p[1] - centroids[k][1];
                double db = p[2] - centroids[k][2];
                double d = dr * dr + dg * dg + db * db;
                if (d < bestDist) { bestDist = d; best = k; }
            }
            if (assignments[i] != best) { assignments[i] = best; changed = true; }
        }
        return changed;
    }

    private void updateCentroids(List<int[]> pixels, int[][] centroids, int[] assignments, int k) {
        long[][] sums = new long[k][3];
        int[] counts = new int[k];
        for (int i = 0; i < pixels.size(); i++) {
            int a = assignments[i];
            sums[a][0] += pixels.get(i)[0];
            sums[a][1] += pixels.get(i)[1];
            sums[a][2] += pixels.get(i)[2];
            counts[a]++;
        }
        for (int c = 0; c < k; c++) {
            if (counts[c] > 0) {
                centroids[c][0] = (int)(sums[c][0] / counts[c]);
                centroids[c][1] = (int)(sums[c][1] / counts[c]);
                centroids[c][2] = (int)(sums[c][2] / counts[c]);
            }
        }
    }
}
