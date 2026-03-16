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
    private static final float DETAIL_STRENGTH = 28f;

    /** Scale of the high-frequency detail noise relative to the sphere. */
    private static final double DETAIL_SCALE = 18.0;

    /**
     * Block size for pixel quantisation.
     * The noise field is sampled once per block and held constant across the block,
     * producing the flat squared-off regions seen in the reference textures.
     * Must be a power of 2. 4 = coarse chunky blocks, 2 = finer blocks.
     */
    private static final int BLOCK_SIZE = 4;

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

        // Step 2 – write pixels using the block-quantised noise value
        for (int by = 0; by < res; by += BLOCK_SIZE) {
            for (int bx = 0; bx < res; bx += BLOCK_SIZE) {
                Vec3 dir   = cubeMapper.toDirection(faceIdx, bx + BLOCK_SIZE / 2, by + BLOCK_SIZE / 2);
                double val = noiseModel.sample(generatorType, dir);
                int noiseGrey = clamp((int)(val * 255));
                int detail = detailOffset(dir);

                for (int dy = 0; dy < BLOCK_SIZE; dy++) {
                    for (int dx = 0; dx < BLOCK_SIZE; dx++) {
                        int x = bx + dx, y = by + dy;
                        if (x >= res || y >= res) continue;

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

                        r = clamp(r + detail);
                        g = clamp(g + detail);
                        b = clamp(b + detail);

                        face.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
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

    private int lerp(int a, int b, float t) {
        return clamp(Math.round(a + t * (b - a)));
    }
}