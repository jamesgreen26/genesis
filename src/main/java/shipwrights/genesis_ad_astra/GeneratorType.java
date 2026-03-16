package shipwrights.genesis_ad_astra;

public enum GeneratorType {
    FRACTAL,   // fractal Perlin noise – terrain / rocky planets
    WORLEY,    // Worley / cellular noise – rocky / icy surfaces
    STRIPES,   // sinusoidal bands – gas giants / striped planets
    HYBRID     // layered mix of fractal + stripes
}
