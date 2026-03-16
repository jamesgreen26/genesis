package shipwrights.genesis_ad_astra;

import java.util.Random;

/**
 * Provides multiple 3D procedural noise generators:
 *   - Fractal Perlin (fBm)
 *   - Worley (cellular)
 *   - Sinusoidal stripes
 *   - Hybrid (stripe + fractal)
 *
 * All methods return a value in [0, 1].
 */
public class NoiseModel {

    // -------------------------------------------------------------------------
    // Perlin noise internals
    // -------------------------------------------------------------------------

    private final int[] perm = new int[512];

    private static final double[] GRAD_X = {
            1,-1, 1,-1, 1,-1, 1,-1, 0, 0, 0, 0, 1,-1, 0, 0
    };
    private static final double[] GRAD_Y = {
            1, 1,-1,-1, 0, 0, 0, 0, 1,-1, 1,-1, 1, 1,-1,-1
    };
    private static final double[] GRAD_Z = {
            0, 0, 0, 0, 1, 1,-1,-1, 1, 1,-1,-1, 0, 0, 1,-1
    };

    public NoiseModel(long seed) {
        Random rng = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        // Fisher-Yates shuffle
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    // -------------------------------------------------------------------------
    // Public API – one method per generator type
    // -------------------------------------------------------------------------

    /**
     * Fractal Brownian Motion (fBm) Perlin noise.
     * Returns [0, 1].
     */
    public double fractal(Vec3 p, int octaves) {
        double value = 0;
        double amp   = 0.5;
        double freq  = 1.0;
        double maxV  = 0;

        for (int i = 0; i < octaves; i++) {
            value += perlin(p.x * freq, p.y * freq, p.z * freq) * amp;
            maxV  += amp;
            amp  *= 0.5;
            freq *= 2.0;
        }
        return (value / maxV + 1.0) * 0.5; // remap [-1,1] → [0,1]
    }

    /**
     * Worley (cellular) noise – returns [0, 1], 0 = near cell centre.
     */
    public double worley(Vec3 p, double scale) {
        double px = p.x * scale;
        double py = p.y * scale;
        double pz = p.z * scale;

        int xi = (int) Math.floor(px);
        int yi = (int) Math.floor(py);
        int zi = (int) Math.floor(pz);

        double minDist = Double.MAX_VALUE;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int cx = xi + dx, cy = yi + dy, cz = zi + dz;
                    // Reproducible random feature point in this cell
                    int hash = hash3(cx, cy, cz);
                    double fx = cx + pseudoRand(hash, 0);
                    double fy = cy + pseudoRand(hash, 1);
                    double fz = cz + pseudoRand(hash, 2);

                    double ddx = px - fx, ddy = py - fy, ddz = pz - fz;
                    double dist = ddx*ddx + ddy*ddy + ddz*ddz;
                    if (dist < minDist) minDist = dist;
                }
            }
        }
        return Math.min(1.0, Math.sqrt(minDist)); // [0, ~1.7] → clamp to [0,1]
    }

    /**
     * Smooth wavy stripe generator, suitable for gas giants.
     *
     * Design goals:
     *   - Stripes run horizontally (constant latitude on the sphere)
     *   - Gentle lateral undulation: the bands wave slightly but never zig-zag
     *   - Varying band widths: achieved by domain-warping the latitude with
     *     a very low-frequency, low-amplitude warp before the sine
     *   - The distortion noise is sampled at LOW frequency and LOW amplitude
     *     so it bends the stripes smoothly rather than shredding them
     *
     * @param p          unit direction vector on the sphere
     * @param frequency  number of stripe pairs around the planet (4–8 is typical)
     * @param warpAmp    how much the stripes undulate laterally (0.04–0.10)
     */
    public double stripes(Vec3 p, double frequency, double warpAmp) {
        // --- Step 1: gentle domain warp ---
        // Sample two independent low-frequency noise values to smoothly
        // displace the latitude coordinate. Using two offsets avoids the
        // "axis-aligned" artefact you get from warping with a single octave.
        double warpU = perlin(p.x * 1.2 + 3.7, p.y * 1.2 + 1.3, p.z * 1.2 + 0.5);
        double warpV = perlin(p.x * 1.1 - 1.5, p.y * 1.1 + 4.2, p.z * 1.1 - 2.1);

        // Remap warp from [0,1] to [-1,+1] and scale by amplitude
        double warpedLat = p.y + (warpU * 2.0 - 1.0) * warpAmp
                + (warpV * 2.0 - 1.0) * warpAmp * 0.4;

        // --- Step 2: width variation ---
        // A second very-low-frequency noise modulates the stripe frequency
        // slightly so bands aren't all the same width.
        double widthMod = 1.0 + (perlin(p.x * 0.6, p.y * 0.6, p.z * 0.6) - 0.5) * 0.25;

        // --- Step 3: smooth sine bands ---
        double raw = Math.sin(warpedLat * Math.PI * frequency * widthMod);

        // Sharpen slightly with a smoothstep so bands have flatter middles
        // and narrower transitions (more gas-giant-like)
        double t = (raw + 1.0) * 0.5;          // [0,1]
        t = smootherstep(t);                    // flatten plateau regions
        return t;
    }

    /**
     * Hybrid: smooth stripes modulated by subtle fractal detail.
     */
    public double hybrid(Vec3 p, double stripeFreq, double warpAmp, int octaves) {
        double s = stripes(p, stripeFreq, warpAmp);
        double f = fractal(p, octaves);
        return s * 0.70 + f * 0.30;
    }

    /** Smootherstep: 6t^5 − 15t^4 + 10t^3 — flattens plateau regions. */
    private double smootherstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // -------------------------------------------------------------------------
    // Dispatch by generator type
    // -------------------------------------------------------------------------

    public double sample(GeneratorType type, Vec3 p) {
        switch (type) {
            case FRACTAL: return fractal(p, 6);
            case WORLEY:  return worley(p, 4.0);
            case STRIPES: return stripes(p, 6.0, 0.06);   // gentle warp amplitude
            case HYBRID:  return hybrid(p, 6.0, 0.06, 4);
            default:      return fractal(p, 6);
        }
    }

    // -------------------------------------------------------------------------
    // Perlin gradient noise internals
    // -------------------------------------------------------------------------

    private double perlin(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x), v = fade(y), w = fade(z);

        int aaa = perm[perm[perm[X  ] + Y  ] + Z  ];
        int aba = perm[perm[perm[X  ] + Y+1] + Z  ];
        int aab = perm[perm[perm[X  ] + Y  ] + Z+1];
        int abb = perm[perm[perm[X  ] + Y+1] + Z+1];
        int baa = perm[perm[perm[X+1] + Y  ] + Z  ];
        int bba = perm[perm[perm[X+1] + Y+1] + Z  ];
        int bab = perm[perm[perm[X+1] + Y  ] + Z+1];
        int bbb = perm[perm[perm[X+1] + Y+1] + Z+1];

        return lerp(w,
                lerp(v,
                        lerp(u, grad(aaa, x,   y,   z  ), grad(baa, x-1, y,   z  )),
                        lerp(u, grad(aba, x,   y-1, z  ), grad(bba, x-1, y-1, z  ))),
                lerp(v,
                        lerp(u, grad(aab, x,   y,   z-1), grad(bab, x-1, y,   z-1)),
                        lerp(u, grad(abb, x,   y-1, z-1), grad(bbb, x-1, y-1, z-1))));
    }

    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private double lerp(double t, double a, double b) { return a + t * (b - a); }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        return GRAD_X[h] * x + GRAD_Y[h] * y + GRAD_Z[h] * z;
    }

    // -------------------------------------------------------------------------
    // Worley helpers
    // -------------------------------------------------------------------------

    private int hash3(int x, int y, int z) {
        return perm[(perm[(perm[Math.floorMod(x, 256)] + Math.floorMod(y, 256)) & 255]
                + Math.floorMod(z, 256)) & 255];
    }

    private double pseudoRand(int hash, int component) {
        // Deterministic jitter in [0,1] from hash + component
        int h = (hash * 1664525 + component * 1013904223) & 0x7FFFFFFF;
        return (h & 0xFFFF) / 65535.0;
    }
}