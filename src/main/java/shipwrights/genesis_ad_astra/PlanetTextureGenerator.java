package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Main entry point for the planet texture generation system.
 *
 * Usage:
 * <pre>
 *   BufferedImage input = ImageIO.read(new File("planet_seed.png")); // 8–32 px
 *   PlanetTextureGenerator gen = new PlanetTextureGenerator();
 *   BufferedImage[] faces = gen.generate(input);
 *   // faces[0..5] are 256×256 cube map textures
 * </pre>
 */
public class PlanetTextureGenerator {

    // -----------------------------------------------------------------------
    // Configuration (can be overridden before calling generate())
    // -----------------------------------------------------------------------

    /** Output resolution per face (square). */
    private int resolution = 256;

    /** Palette size (number of dominant colours to extract). */
    private int paletteSize = 16;

    /** Whether to apply dithering during palette remapping. */
    private boolean dither = true;

    /** Random seed for noise generation. */
    private long noiseSeed = 12345L;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generate six seamless cube-face textures from a small input texture,
     * using the planet name to deterministically seed the noise offset so
     * that different planets look distinct even with the same generator type.
     *
     * @param inputTexture a 8–32 px RGB image
     * @param planetName   any stable identifier (filename, registry key, etc.)
     */
    public BufferedImage[] generate(BufferedImage inputTexture, String planetName) {
        // Combine the manual seed with the name's hashcode so:
        //   - Same name + same seed → identical output (deterministic)
        //   - Different names → different noise offset even with same seed
        long combinedSeed = noiseSeed ^ ((long) planetName.hashCode() * 0x9e3779b97f4a7c15L);
        return generateWithSeed(inputTexture, combinedSeed);
    }

    /**
     * Generate using only the manual seed (no name). Kept for compatibility.
     */
    public BufferedImage[] generate(BufferedImage inputTexture) {
        return generateWithSeed(inputTexture, noiseSeed);
    }

    private BufferedImage[] generateWithSeed(BufferedImage inputTexture, long seed) {
        long start = System.currentTimeMillis();

        // -----------------------------------------------------------------
        // 1. Palette extraction
        // -----------------------------------------------------------------
        log("Step 1: Palette extraction...");
        PaletteExtractor extractor = new PaletteExtractor();
        List<PaletteColor> palette = extractor.extract(inputTexture, paletteSize);
        log("  Extracted " + palette.size() + " palette colours.");

        // -----------------------------------------------------------------
        // 2. Pattern analysis
        // -----------------------------------------------------------------
        log("Step 2: Pattern analysis...");
        PatternAnalyzer analyzer = new PatternAnalyzer();
        PatternAnalyzer.PatternDescriptor pattern = analyzer.analyze(inputTexture);
        log("  " + pattern);

        // -----------------------------------------------------------------
        // 3. Generator selection
        // -----------------------------------------------------------------
        GeneratorType genType = pattern.recommended;
        log("Step 3: Selected generator: " + genType);

        // -----------------------------------------------------------------
        // 4. Build patch library
        // -----------------------------------------------------------------
        log("Step 4: Building patch library...");
        PatchLibrary patchLibrary = new PatchLibrary(inputTexture);
        log("  Patch library contains " + patchLibrary.size() + " patches.");

        // -----------------------------------------------------------------
        // 5. Synthesize six cube faces
        // -----------------------------------------------------------------
        log("Step 5: Synthesizing " + resolution + "x" + resolution + " faces...");
        CubeMapper   cubeMapper = new CubeMapper(resolution);
        NoiseModel   noiseModel = new NoiseModel(seed);
        TextureSynthesizer synth = new TextureSynthesizer(cubeMapper, noiseModel, patchLibrary, genType);

        BufferedImage[] faces = synth.synthesize();

        // -----------------------------------------------------------------
        // 6. Palette remapping
        // -----------------------------------------------------------------
        log("Step 6: Palette remapping...");
        PaletteMapper mapper = new PaletteMapper(palette, dither);
        mapper.remap(faces);

        // -----------------------------------------------------------------
        // 7. Seam fixing
        // -----------------------------------------------------------------
        log("Step 7: Fixing seams...");
        SeamFixer seamFixer = new SeamFixer();
        seamFixer.fix(faces);

        long elapsed = System.currentTimeMillis() - start;
        log("Done! Total time: " + elapsed + " ms");
        return faces;
    }

    // -----------------------------------------------------------------------
    // Fluent configuration setters
    // -----------------------------------------------------------------------

    public PlanetTextureGenerator resolution(int res) {
        this.resolution = res;
        return this;
    }

    public PlanetTextureGenerator paletteSize(int size) {
        this.paletteSize = size;
        return this;
    }

    public PlanetTextureGenerator dither(boolean enabled) {
        this.dither = enabled;
        return this;
    }

    public PlanetTextureGenerator seed(long seed) {
        this.noiseSeed = seed;
        return this;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void log(String msg) {
        System.out.println("[PlanetGen] " + msg);
    }
}