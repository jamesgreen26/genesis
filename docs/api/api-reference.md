# API Reference

Complete reference for the Genesis celestial system.

## Table of Contents

- [Celestial](#celestial)
- [CelestialType](#celestialtype)
- [CelestialTransformProvider](#celestialtransformprovider)
- [StaticTransformProvider](#statictransformprovider)
- [OrbitingTransformProvider](#orbitingtransformprovider)
- [CelestialProperties](#celestialproperties)
- [VantagePoint](#vantagepoint)
- [SpaceLevel](#spacelevel)
- [GenesisMod](#genesismod)
- [JSON Schema](#json-schema)

---

## Celestial

**Package:** `shipwrights.genesis.space`

`Celestial` is a **record** representing an astronomical body.

### Record Components

```java
public record Celestial(
    CelestialTransformProvider transformProvider,
    CelestialType type,
    double size,
    double gravity,
    float r,
    float g,
    float b,
    @NotNull CelestialProperties properties
)
```

### Methods

```java
Vector3dc getPosition(long ticks, float partialTick, Registry<Celestial> registry)
Vector3dc getPosition(long ticks, Registry<Celestial> registry)

Quaterniondc getRotation(long ticks, float partialTick, Registry<Celestial> registry)

OBB getOBB(long ticks, Registry<Celestial> registry)
OBB getOBB(long ticks, float subticks, Registry<Celestial> registry)

double getActualSize()   // equivalent to size()

Celestial getNearestStar(long gameTime, float partialTick, Registry<Celestial> registry)
// Returns this if this is a star. Throws IllegalStateException if no stars exist.
```

### Static Fields

```java
public static final Codec<Celestial> CODEC
```

---

## CelestialType

**Package:** `shipwrights.genesis.space.type`

Interface defining rendering and behavior for a celestial type.

### Methods

```java
boolean castsLight()    // true for stars/light sources
boolean castsShadow()   // true for opaque bodies
boolean isVisitable()   // true if players can land on it
@NotNull CelestialRenderer getRenderer()
@NotNull ResourceLocation getID()
@NotNull Codec<? extends CelestialProperties> propertiesCodec()
```

### Static Methods

```java
static CelestialType get(ResourceLocation id)       // look up by ID
static void register(CelestialType type)            // register a custom type
```

### Built-in Types

| ID | `castsLight` | `castsShadow` | `isVisitable` |
|----|-------------|--------------|--------------|
| `genesis:star` | true | false | false |
| `genesis:body` | false | true | true |
| `genesis:blackhole` | false | false | false |

---

## CelestialTransformProvider

**Package:** `shipwrights.genesis.space.transformProvider`

Interface for position and rotation over time.

### Methods

```java
Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry)
Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry)
ResourceLocation getType()
```

### Static Methods

```java
static void register(ResourceLocation type, Codec<? extends CelestialTransformProvider> codec)
```

### Static Fields

```java
Codec<CelestialTransformProvider> DISPATCH_CODEC   // dispatch codec for JSON
```

---

## StaticTransformProvider

**Type ID:** `genesis:static`

### Constructors

```java
public StaticTransformProvider(double x, double y, double z)
public StaticTransformProvider(double x, double y, double z, double xRot, double yRot, double zRot)
```

Rotation values are in radians. Defaults to `0.0`.

### JSON Fields

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `x`, `y`, `z` | Number | Yes | — |
| `xRot`, `yRot`, `zRot` | Number | No | `0.0` |

---

## OrbitingTransformProvider

**Type ID:** `genesis:orbiting`

### Constructor

```java
public OrbitingTransformProvider(
    ResourceLocation parentID,
    int seed,
    double orbitDistance,
    double orbitTime,
    double dayLength
)
```

### JSON Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `parentID` | String | Yes | — | ID of the celestial to orbit |
| `seed` | Integer | Yes | — | Seed for deterministic orbital angles |
| `orbitDistance` | Number | Yes | — | Orbit radius in blocks |
| `orbitTime` | Number | Yes | — | Orbital period in ticks |
| `dayLength` | Number | No | `1.0` | Axial rotation period in ticks; `0` = tidally locked |

---

## CelestialProperties

**Package:** `shipwrights.genesis.space.properties`

Marker interface. Each `CelestialType` provides its own implementation via `propertiesCodec()`.

### StarProperties

```java
public record StarProperties(int r0, int g0, int b0, int r1, int g1, int b1)
```

Two-color gradient (RGB 0–255). Fields match directly as JSON keys.

### PlanetProperties

```java
public record PlanetProperties(Atmosphere atmosphere)
```

### Atmosphere

```java
public record Atmosphere(
    double density,
    double thickness,
    boolean precipitation,
    boolean isBreathable,
    PlanetColorPalette color
)
```

| Field | Description |
|-------|-------------|
| `density` | Atmosphere density (affects fade when leaving) |
| `thickness` | Visual thickness of atmosphere shell |
| `precipitation` | Whether weather occurs |
| `isBreathable` | Whether players can breathe |
| `color` | `{ "type": "genesis:overworld" }` or `{ "type": "genesis:rgb", "r": 0, "g": 100, "b": 255 }` |

### EmptyProperties

Used by `genesis:blackhole`. Codec produces `{}` / accepts any object.

---

## VantagePoint

**Package:** `shipwrights.genesis.space`

Describes the observer's position and orientation for rendering. Passed to `CelestialRenderer.invoke()`.

### Methods

```java
Vector3dc getPosition()
Quaterniondc getRotation()
```

### Static Factory

```java
@Nullable static VantagePoint get(Level level, Vector3dc posInLevel, long ticks, float partialTick)
// Returns null if the level has no space-level association
```

### Implementations

**`VantagePoint.InSpace`** — observer is in space (not on a celestial surface).

**`VantagePoint.OnCelestial`** — observer is standing on a celestial.
- `Celestial celestial` — the body the observer is on
- `getCelestialRotation()` — the celestial's own rotation

---

## SpaceLevel

**Package:** `shipwrights.genesis.space`

Static utility for spatial queries.

```java
@Nullable static Pair<Celestial, Double> nearestCelestialWhere(
    Registry<Celestial> registry,
    Vector3dc position,
    long ticks,
    float partialTick,
    Predicate<CelestialType> predicate
)
// Returns the nearest matching celestial and its distance squared, or null
```

```java
@Nullable static Pair<Celestial, Double> celestialRaycast(
    Registry<Celestial> registry,
    long ticks,
    float partialTick,
    Vector3d origin,
    Vector3d direction,
    Predicate<CelestialType> predicate
)
// Returns the first hit celestial and distance squared along the ray, or null
```

---

## GenesisMod

**Package:** `shipwrights.genesis`

### Static Fields

```java
public static final ResourceKey<Registry<Celestial>> CELESTIALS_KEY
```

### Static Methods

```java
// Get the celestial registry for a level
public static Registry<Celestial> getCelestialRegistry(Level level)

// Get the celestial whose ID matches the level's dimension (e.g. minecraft:overworld → Overworld celestial)
@Nullable public static Celestial getCelestialForLevel(Level level)
```

---

## JSON Schema

### File Location

```
data/<namespace>/genesis/celestials/<name>.json
```

The celestial's ID is `namespace:name`. One celestial per file.

### Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | — | Celestial type ID |
| `size` | Number | Yes | — | Diameter in blocks |
| `gravity` | Number | Yes | — | Gravity multiplier (1.0 = Earth) |
| `r`, `g`, `b` | Number | No | `0.5` | Color (0.0–1.0), reserved for future use |
| `transformProvider` | Object | Yes | — | Transform — see [StaticTransformProvider](#statictransformprovider) / [OrbitingTransformProvider](#orbitingtransformprovider) |
| `properties` | Object | Yes | — | Type-specific — see [CelestialProperties](#celestialproperties) |
