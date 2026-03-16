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

        // Step 1 – noise pass: compute noise value per pixel, scatter patches
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                Vec3 dir   = cubeMapper.toDirection(faceIdx, x, y);
                double val = noiseModel.sample(generatorType, dir);

                Patch patch = patchLibrary.match(val);
                int half    = patch.size / 2;

                // Place patch centred on (x, y)
                for (int pr = 0; pr < patch.size; pr++) {
                    for (int pc = 0; pc < patch.size; pc++) {
                        int tx = x + pc - half;
                        int ty = y + pr - half;
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

        // Step 2 – write pixels: blend patch accumulation with direct noise colour
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                Vec3 dir   = cubeMapper.toDirection(faceIdx, x, y);
                double val = noiseModel.sample(generatorType, dir);

                // Noise-derived grey (will be palette-remapped later)
                int noiseGrey = clamp((int)(val * 255));

                int pr, pg, pb;
                if (totalW[y][x] > 0) {
                    pr = clamp((int)(weightR[y][x] / totalW[y][x]));
                    pg = clamp((int)(weightG[y][x] / totalW[y][x]));
                    pb = clamp((int)(weightB[y][x] / totalW[y][x]));
                } else {
                    pr = pg = pb = noiseGrey;
                }

                // Blend patch colour with noise grey
                int r = lerp(noiseGrey, pr, PATCH_BLEND);
                int g = lerp(noiseGrey, pg, PATCH_BLEND);
                int b = lerp(noiseGrey, pb, PATCH_BLEND);

                face.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
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
