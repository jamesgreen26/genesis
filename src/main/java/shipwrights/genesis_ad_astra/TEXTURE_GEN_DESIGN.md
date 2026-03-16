# Planet Texture Generation Design (Minecraft Java Mod)

## Overview

This document describes the design of a procedural system that generates **six 256×256 cube-map textures for a planet** from a **single low-resolution input texture (8×8 to 32×32)**.

The goals of the system are:

* Preserve the **color palette** of the input texture
* Replicate **visible structures** such as stripes, blobs, or gradients
* Produce **six seamless cube faces**
* Complete generation in **≤5 seconds**
* Remain **simple and maintainable (<10k lines of code)**

The system uses a **hybrid procedural texture synthesis pipeline**, combining palette extraction, pattern analysis, procedural noise, and patch-based synthesis.

---

# High Level Pipeline

```
Input Texture (8–32 px)
        │
        ▼
Palette Extraction
        │
        ▼
Pattern Analysis
        │
        ▼
Procedural Generator Selection
        │
        ▼
Cube-Mapped Texture Generation
        │
        ▼
Patch-Based Texture Synthesis
        │
        ▼
Palette Remapping
        │
        ▼
Seam Fixing
        │
        ▼
Output (6 × 256×256 textures)
```

---

# Input

The generator receives:

```
BufferedImage inputTexture
```

Constraints:

| Property        | Value                            |
| --------------- | -------------------------------- |
| Width           | 8–32 pixels                      |
| Height          | 8–32 pixels                      |
| Format          | RGB                              |
| Texture meaning | One face of a cube-mapped planet |

The texture may contain:

* stripes
* blobs
* noisy terrain patterns
* gradients

The generator attempts to preserve these features.

---

# Core Design Principles

### 1. Preserve the palette

The original colors define the planet style.

### 2. Replicate macro-patterns

Large structures (bands, blobs) should reappear in the generated textures.

### 3. Procedural detail

Noise adds believable high-resolution structure.

### 4. Seamless cube mapping

All six faces should connect smoothly.

---

# Step 1: Palette Extraction

The first step extracts the dominant colors from the input texture.

## Method

1. Read all pixels
2. Cluster them using **k-means** or **median cut**
3. Generate a palette of **16–32 colors**

Example palette structure:

```
class PaletteColor {
    int r;
    int g;
    int b;
    float weight;
}
```

The palette is later used to **remap generated colors** so the output always matches the input style.

## Palette Mapping

After generation, each pixel is mapped to the closest palette color:

```
color = nearestPaletteColor(color)
```

This guarantees visual consistency.

---

# Step 2: Pattern Analysis

The system analyzes the low-resolution texture to determine which procedural generator should be used.

## Metrics

### Row variance

Detects horizontal stripes.

```
rowVariance[y]
```

Large periodic variation suggests banded patterns.

### Column variance

Detects vertical stripes.

```
colVariance[x]
```

### Gradient orientation histogram

Measures the dominant gradient direction.

Used to detect:

* horizontal banding
* vertical banding
* isotropic noise

### Blob detection

Connected component analysis is used to measure:

* average blob size
* blob density
* blob roundness

This helps detect rocky or cellular patterns.

---

# Step 3: Procedural Generator Selection

Based on the pattern analysis, the system selects one of several procedural generators.

| Pattern        | Generator        |
| -------------- | ---------------- |
| smooth terrain | fractal noise    |
| blobs          | Worley noise     |
| stripes        | sinusoidal bands |
| mixed patterns | layered noise    |

Example enum:

```
enum GeneratorType {
    FRACTAL,
    WORLEY,
    STRIPES,
    HYBRID
}
```

---

# Step 4: Cube Map Sampling

To avoid seams between faces, the system samples a **single 3D procedural field** rather than generating faces independently.

## Process

For each pixel in each face:

1. Convert cube coordinates → 3D direction vector
2. Sample procedural noise at that vector

```
Vec3 dir = cubeToVector(face, x, y);
value = noise3D(dir * scale);
```

Because all faces share the same 3D space, edges align naturally.

---

# Cube Face Coordinate Conversion

Example implementation:

```
Vec3 cubeToVector(int face, int x, int y) {
    float u = (x / 255f) * 2f - 1f;
    float v = (y / 255f) * 2f - 1f;

    switch(face) {
        case 0: return normalize( 1,  v, -u);
        case 1: return normalize(-1,  v,  u);
        case 2: return normalize( u,  1, -v);
        case 3: return normalize( u, -1,  v);
        case 4: return normalize( u,  v,  1);
        case 5: return normalize(-u,  v, -1);
    }
}
```

---

# Step 5: Procedural Noise

Procedural noise creates high-resolution detail.

## Fractal Noise

Fractal noise is created by summing multiple octaves.

```
noise =
    perlin(p * 1.0) +
    perlin(p * 2.0) * 0.5 +
    perlin(p * 4.0) * 0.25 +
    perlin(p * 8.0) * 0.125
```

This produces natural terrain-like structure.

## Stripe Generator

Used for gas giant planets.

```
value = sin(latitude * bandFrequency + noise * distortion)
```

## Worley Noise

Useful for cellular or rocky patterns.

```
value = worley(p)
```

---

# Step 6: Patch-Based Texture Synthesis

Procedural noise alone cannot reproduce recognizable shapes from the input.

To solve this, the generator extracts **small patches** from the input texture and reuses them.

## Patch Library

Patches are extracted from the source texture.

Example sizes:

```
3×3
4×4
5×5
```

Example structure:

```
class Patch {
    int size;
    int[][] colors;
    float averageValue;
}
```

Each patch stores:

* pixel data
* average brightness or palette index

---

# Patch Placement

During generation:

1. Compute the procedural value
2. Select a patch with a similar average value
3. Blend the patch into the output texture

Pseudo code:

```
Patch p = patchLibrary.match(value);

for(px = 0; px < p.size; px++)
    for(py = 0; py < p.size; py++)
        output[x+px][y+py] = blend(output[x+px][y+py], p.pixel(px,py));
```

This transfers recognizable shapes from the source texture.

---

# Step 7: Palette Remapping

After synthesis, colors are remapped to the original palette.

```
for each pixel:
    pixel = nearestPaletteColor(pixel)
```

Optional: apply dithering to reduce banding.

---

# Step 8: Seam Fixing

Cube edges may still show minor seams.

A simple fix:

1. Copy border pixels between adjacent faces
2. Blend a small region (2–4 pixels)

Example:

```
blendEdge(faceA, faceB)
```

Because the generator uses 3D noise sampling, seams should already be minimal.

---

# Performance Expectations

Texture size:

```
6 faces × 256 × 256 = 393,216 pixels
```

Estimated runtime:

| Stage              | Time    |
| ------------------ | ------- |
| palette extraction | <5 ms   |
| pattern analysis   | <5 ms   |
| noise generation   | ~30 ms  |
| patch synthesis    | ~200 ms |
| palette remap      | ~20 ms  |

Total expected runtime:

```
~250–500 ms
```

This is well under the **5 second requirement**.

---

# Suggested Code Structure

```
planetgen/

PlanetTextureGenerator.java
PaletteExtractor.java
PaletteMapper.java
PatternAnalyzer.java
NoiseModel.java
CubeMapper.java
PatchLibrary.java
TextureSynthesizer.java
SeamFixer.java
```

Total expected implementation size:

```
2,000 – 4,000 lines of code
```

---

# Core Generation Pseudocode

```
generatePlanet(inputTexture):

    palette = extractPalette(inputTexture)

    pattern = analyzePattern(inputTexture)

    generator = selectGenerator(pattern)

    patchLibrary = buildPatchLibrary(inputTexture)

    for face in 0..5:
        for y in 0..255:
            for x in 0..255:

                dir = cubeToVector(face, x, y)

                value = generator.sample(dir)

                patch = patchLibrary.match(value)

                applyPatch(output[face], x, y, patch)

    paletteRemap(output)

    fixSeams(output)

    return output
```

---

# Future Improvements

Possible enhancements that remain simple:

### Latitude climate zones

Different palettes per latitude band.

### Multi-scale patch libraries

Separate patches for:

* large features
* small features

### Biome-style generation

Blend multiple procedural generators.

### GPU acceleration

Move generation into an OpenGL shader for near-instant generation.

---

# Summary

This system provides a practical way to generate planet textures from extremely small inputs.

Key advantages:

* Preserves **palette and visual identity**
* Replicates **recognizable shapes**
* Produces **seamless cube maps**
* Runs in **<1 second**
* Fits comfortably within **a few thousand lines of Java code**

The result is a flexible planet texture generator suitable for Minecraft mod environments.
