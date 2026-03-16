package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;

/**
 * Core synthesis loop.
 *
 * For each pixel on each cube face:
 *   1. Convert cube coordinates → 3D direction vector
 *   2. Sample the chosen procedural noise generator
 *   3. Select the nearest patch from the patch library
 *   4. Blend the patch into the output image
 */
public class TextureSynthesizer {

    private final CubeMapper  cubeMapper;
    private final NoiseModel  noiseModel;
    private final PatchLibrary patchLibrary;
    private final GeneratorType generatorType;

    /** Controls how strongly patch pixels override noise-derived colours. [0=noise only, 1=patch only] */
    private static final float PATCH_BLEND = 0.72f;

    /**
     * Detail noise strength: signed offset added to each channel before palette
     * remapping to break up large flat colour regions.
     * Range ±DETAIL_STRENGTH in [0,255] space.
     */
    private static final float DETAIL_STRENGTH = 16f;

    /** Scale of the high-frequency detail noise relative to the sphere. */
    private static final double DETAIL_SCALE = 26.0;

    /**
     * Block size for pixel quantisation.
     * The noise field is sampled once per block and held constant across the block,
     * producing the flat squared-off regions seen in the reference textures.
     * Must be a power of 2. 32 = very chunky blocks, 16 = medium, 8 = finer blocks.
     */
    private static final int BLOCK_SIZE = 32;

    /** Block size variation range (average remains BLOCK_SIZE). */
    private static final int BLOCK_SIZE_VARIATION = 8;

    /** Noise scale that drives block-size changes (lower = bigger regions). */
    private static final double BLOCK_SIZE_NOISE_SCALE = 0.9;

    /** Per-block origin jitter to avoid low-res grid alignment. */
    private static final int BLOCK_JITTER = 8;

    /** Noise scale that drives block jitter. */
    private static final double BLOCK_JITTER_SCALE = 1.5;

    /** How much of the fine noise to mix into the coarse block value. */
    private static final double DETAIL_BLEND = 0.18;

    public TextureSynthesizer(
            CubeMapper    cubeMapper,
            NoiseModel    noiseModel,
            PatchLibrary  patchLibrary,
            GeneratorType generatorType) {
        this.cubeMapper    = cubeMapper;
        this.noiseModel    = noiseModel;
        this.patchLibrary  = patchLibrary;
        this.generatorType = generatorType;
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    /**
     * Generate all six cube face textures and return them as an array indexed 0–5.
     */
    public BufferedImage[] synthesize() {
        int res = cubeMapper.getResolution();
        BufferedImage[] faces = new BufferedImage[6];
        for (int f = 0; f < 6; f++) {
            faces[f] = new BufferedImage(res, res, BufferedImage.TYPE_INT_RGB);
        }

        for (int f = 0; f < 6; f++) {
            synthesizeFace(faces[f], f, res);
        }

        return faces;
    }

    // -------------------------------------------------------------------------
    // Per-face synthesis
    // -------------------------------------------------------------------------

    private void synthesizeFace(BufferedImage face, int faceIdx, int res) {
        // We accumulate blend weights in a separate weight buffer so multiple
        // overlapping patches average out gracefully.
        float[][] weightR = new float[res][res];
        float[][] weightG = new float[res][res];
        float[][] weightB = new float[res][res];
        float[][] totalW  = new float[res][res];

        // Step 1 – noise pass: sample noise ONCE PER BLOCK and fill all pixels in
        // that block with the same value. This creates the flat squared-off colour
        // regions characteristic of the reference pixel-art style.
        for (int by = 0; by < res; by += BLOCK_SIZE) {
            for (int bx = 0; bx < res; bx += BLOCK_SIZE) {
                // Sample at the block centre
                Vec3 dir = cubeMapper.toDirection(faceIdx, bx + BLOCK_SIZE / 2, by + BLOCK_SIZE / 2);
                double val = noiseModel.sample(generatorType, dir);

                Patch patch = patchLibrary.match(val);
                int half    = patch.size / 2;

                // Scatter patch centred on the block centre, snapped to block grid
                for (int pr = 0; pr < patch.size; pr++) {
                    for (int pc = 0; pc < patch.size; pc++) {
                        // Snap target position to block grid
                        int rawX = bx + (pc - half) * BLOCK_SIZE;
                        int rawY = by + (pr - half) * BLOCK_SIZE;
                        // Fill every pixel in the target block
                        for (int dy = 0; dy < BLOCK_SIZE; dy++) {
                            for (int dx = 0; dx < BLOCK_SIZE; dx++) {
                                int tx = rawX + dx;
                                int ty = rawY + dy;
                                if (tx < 0 || tx >= res || ty < 0 || ty >= res) continue;
                                float w = patchWeight(pc - half, pr - half, patch.size);
                                weightR[ty][tx] += patch.getR(pr, pc) * w;
                                weightG[ty][tx] += patch.getG(pr, pc) * w;
                                weightB[ty][tx] += patch.getB(pr, pc) * w;
                                totalW [ty][tx] += w;
                            }
                        }
                    }
                }
            }
        }

        // Step 2 – write pixels using a jittered, variable-size block field
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                int cellX = x / BLOCK_SIZE;
                int cellY = y / BLOCK_SIZE;
                int cellOriginX = cellX * BLOCK_SIZE;
                int cellOriginY = cellY * BLOCK_SIZE;
                int cellCenterX = clamp(cellOriginX + BLOCK_SIZE / 2, 0, res - 1);
                int cellCenterY = clamp(cellOriginY + BLOCK_SIZE / 2, 0, res - 1);
                Vec3 cellDir = cubeMapper.toDirection(faceIdx, cellCenterX, cellCenterY);

                // Vary block size per region but keep average at BLOCK_SIZE
                double sizeNoise = noiseModel.fractal(cellDir.scale(BLOCK_SIZE_NOISE_SCALE), 1);
                int size = BLOCK_SIZE - BLOCK_SIZE_VARIATION
                        + (int) Math.round(sizeNoise * (BLOCK_SIZE_VARIATION * 2.0));
                if (size < 4) size = 4;

                // Jitter the block origin to avoid low-res grid alignment
                double jitterA = noiseModel.fractal(cellDir.scale(BLOCK_JITTER_SCALE), 1);
                double jitterB = noiseModel.fractal(cellDir.scale(BLOCK_JITTER_SCALE + 5.3), 1);
                int jx = (int) Math.round((jitterA * 2.0 - 1.0) * BLOCK_JITTER);
                int jy = (int) Math.round((jitterB * 2.0 - 1.0) * BLOCK_JITTER);

                int bx = cellOriginX + jx;
                int by = cellOriginY + jy;

                int cx = clamp(bx + size / 2, 0, res - 1);
                int cy = clamp(by + size / 2, 0, res - 1);
                Vec3 baseDir = cubeMapper.toDirection(faceIdx, cx, cy);
                double baseVal = noiseModel.sample(generatorType, baseDir);

                Vec3 dir = cubeMapper.toDirection(faceIdx, x, y);
                double fineVal = noiseModel.sample(generatorType, dir);
                double val = baseVal + (fineVal - baseVal) * DETAIL_BLEND;
                int noiseGrey = clamp((int) (val * 255));

                int pr, pg, pb;
                if (totalW[y][x] > 0) {
                    pr = clamp((int)(weightR[y][x] / totalW[y][x]));
                    pg = clamp((int)(weightG[y][x] / totalW[y][x]));
                    pb = clamp((int)(weightB[y][x] / totalW[y][x]));
                } else {
                    pr = pg = pb = noiseGrey;
                }

                int r = lerp(noiseGrey, pr, PATCH_BLEND);
                int g = lerp(noiseGrey, pg, PATCH_BLEND);
                int b = lerp(noiseGrey, pb, PATCH_BLEND);

                int detail = detailOffset(dir);
                r = clamp(r + detail);
                g = clamp(g + detail);
                b = clamp(b + detail);

                face.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Detail noise
    // -------------------------------------------------------------------------

    /**
     * Returns a signed integer offset in [-DETAIL_STRENGTH, +DETAIL_STRENGTH]
     * computed from two octaves of high-frequency fractal noise.
     * Applied to all channels equally so hue is preserved while brightness varies.
     */
    private int detailOffset(Vec3 dir) {
        // Two octaves at high frequency
        double n = noiseModel.fractal(dir.scale(DETAIL_SCALE), 2);
        // n is in [0,1]; remap to [-1,+1] then scale
        return Math.round((float)((n * 2.0 - 1.0) * DETAIL_STRENGTH));
    }

    // -------------------------------------------------------------------------
    // Patch weight: Gaussian-like fall-off from patch centre
    // -------------------------------------------------------------------------

    private float patchWeight(int dx, int dy, int size) {
        float sigma = size / 3.0f;
        return (float) Math.exp(-(dx*dx + dy*dy) / (2 * sigma * sigma));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private int lerp(int a, int b, float t) {
        return clamp(Math.round(a + t * (b - a)));
    }
}
