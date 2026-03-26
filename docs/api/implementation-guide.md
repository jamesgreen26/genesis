# Implementation Guide: Creating Celestials

Celestials are defined as individual JSON files using Minecraft's datapack registry:

```
data/<namespace>/genesis/celestials/<name>.json
```

Each file defines one celestial. Its ID is `namespace:name`. You can add files from a datapack or from within a mod's `resources/` directory.

---

## A Star

`data/mymod/genesis/celestials/sun.json`

```json
{
  "type": "genesis:star",
  "size": 1440,
  "gravity": 2.0,
  "transformProvider": {
    "type": "genesis:static",
    "x": 0.0,
    "y": 0.0,
    "z": 0.0,
    "xRot": 15,
    "yRot": 45,
    "zRot": 5
  },
  "properties": {
    "r0": 255, "g0": 220, "b0": 150,
    "r1": 255, "g1": 180, "b1": 80
  }
}
```

`properties` for `genesis:star` is a two-color gradient rendered on the star's surface (values 0–255).

---

## An Orbiting Planet

`data/mymod/genesis/celestials/earth.json`

```json
{
  "type": "genesis:body",
  "size": 96,
  "gravity": 1.0,
  "transformProvider": {
    "type": "genesis:orbiting",
    "parentID": "mymod:sun",
    "seed": 12345,
    "orbitDistance": 15000,
    "orbitTime": 4600000,
    "dayLength": 24000
  },
  "properties": {
    "atmosphere": {
      "density": 1.0,
      "thickness": 1.0,
      "precipitation": true,
      "isBreathable": true,
      "color": { "type": "genesis:overworld" }
    }
  }
}
```

`parentID` references the ID of the celestial to orbit. The planet will follow its parent as that parent moves.

---

## A Moon

`data/mymod/genesis/celestials/moon.json`

```json
{
  "type": "genesis:body",
  "size": 29,
  "gravity": 0.1622,
  "transformProvider": {
    "type": "genesis:orbiting",
    "parentID": "mymod:earth",
    "seed": 67890,
    "orbitDistance": 750,
    "orbitTime": 184329,
    "dayLength": 0
  },
  "properties": {
    "atmosphere": {
      "density": 0.0,
      "thickness": 0.0,
      "precipitation": false,
      "isBreathable": false,
      "color": { "type": "genesis:rgb", "r": 5, "g": 5, "b": 10 }
    }
  }
}
```

`dayLength: 0` enables tidal locking — the same face always points toward the parent.

---

## A Black Hole

`data/mymod/genesis/celestials/blackhole.json`

```json
{
  "type": "genesis:blackhole",
  "size": 2048,
  "gravity": 8.0,
  "transformProvider": {
    "type": "genesis:static",
    "x": 0.0,
    "y": -200000.0,
    "z": -200000.0
  },
  "properties": {}
}
```

`genesis:blackhole` has no type-specific properties.

---

## Transform Providers

### `genesis:static`

Fixed position and optional rotation.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `x`, `y`, `z` | Number | Yes | — | Position |
| `xRot`, `yRot`, `zRot` | Number | No | `0.0` | Rotation in radians |

### `genesis:orbiting`

Circular orbit around a parent.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `parentID` | String | Yes | — | ID of celestial to orbit |
| `seed` | Integer | Yes | — | Random seed for orbital angles (same seed = same orbit) |
| `orbitDistance` | Number | Yes | — | Orbit radius in blocks |
| `orbitTime` | Number | Yes | — | Orbital period in ticks |
| `dayLength` | Number | No | `1.0` | Axial rotation period in ticks; `0` = tidally locked |

---

## Celestial Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | — | Celestial type ID |
| `size` | Number | Yes | — | Diameter in blocks |
| `gravity` | Number | Yes | — | Gravity multiplier (1.0 = Earth) |
| `r`, `g`, `b` | Number | No | `0.5` | Color components (0.0–1.0), reserved for future use |
| `transformProvider` | Object | Yes | — | Transform configuration |
| `properties` | Object | Yes | — | Type-specific properties (see below) |

### `genesis:star` properties

| Field | Type | Description |
|-------|------|-------------|
| `r0`, `g0`, `b0` | Integer (0–255) | Primary gradient color |
| `r1`, `g1`, `b1` | Integer (0–255) | Secondary gradient color |

### `genesis:body` properties

```json
"properties": {
  "atmosphere": {
    "density": 1.0,
    "thickness": 1.0,
    "precipitation": true,
    "isBreathable": true,
    "color": { "type": "genesis:overworld" }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `density` | Number | Atmosphere density (affects transition when leaving) |
| `thickness` | Number | Visual thickness of atmosphere shell |
| `precipitation` | Boolean | Whether weather occurs |
| `isBreathable` | Boolean | Whether players can breathe |
| `color` | Object | `{ "type": "genesis:overworld" }` or `{ "type": "genesis:rgb", "r": 0, "g": 100, "b": 255 }` |

### `genesis:blackhole` properties

Use `"properties": {}`.

---

## Reference Values

Genesis default solar system values:

| Parameter | Value | Notes |
|-----------|-------|-------|
| `orbitDistance` (Earth-Sun) | `15000` | blocks |
| `orbitTime` (Earth year) | `4600000` | ticks ≈ 64 hours |
| `dayLength` (Earth day) | `24000` | ticks = 20 minutes |
| `size` (Sun) | `1440` | blocks |
| `size` (Overworld) | `96` | blocks |
| `size` (Moon) | `29` | blocks |
| `gravity` (Moon) | `0.1622` | ~1/6 Earth |

---

## Textures

Planet and moon textures (`genesis:body`) are looked up automatically by celestial ID. Place them in a **resource pack or mod** (client-side):

```
assets/genesis/textures/planets/<namespace>/<name>.png
```

For example, `mymod:earth` → `assets/genesis/textures/planets/mymod/earth.png`.

The texture uses a **cube map** in a 3×2 grid:

```
┌─────────┬─────────┬─────────┐
│  North  │  West   │  South  │
├─────────┼─────────┼─────────┤
│  East   │  Down   │   Up    │
└─────────┴─────────┴─────────┘
```

Use dimensions divisible by 3×2 — powers of 2 work well (e.g. 192×128, 384×256).

---

## Querying Celestials at Runtime

Access the registry from a `Level`:

```java
Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

// Get by ID
Celestial sun = registry.get(ResourceLocation.parse("genesis:sun"));

// Get the celestial for the current level/dimension
Celestial current = GenesisMod.getCelestialForLevel(level);

// Get all
registry.stream().forEach(c -> { ... });

// Filter — e.g. all stars
registry.stream()
    .filter(c -> c.type().castsLight())
    .forEach(c -> { ... });

// Filter — e.g. all visitable bodies
registry.stream()
    .filter(c -> c.type().isVisitable())
    .forEach(c -> { ... });
```

---

## Complete Example: Genesis Default Solar System

The four files that ship with Genesis:

`data/genesis/genesis/celestials/sun.json`
```json
{
  "type": "genesis:star",
  "size": 1440,
  "gravity": 2.0,
  "transformProvider": {
    "type": "genesis:static",
    "x": 0.0, "y": 0.0, "z": 0.0,
    "xRot": 15, "yRot": 45, "zRot": 5
  },
  "properties": {
    "r0": 255, "g0": 0, "b0": 0,
    "r1": 255, "g1": 204, "b1": 38
  }
}
```

`data/minecraft/genesis/celestials/overworld.json`
```json
{
  "type": "genesis:body",
  "size": 96,
  "gravity": 1.0,
  "transformProvider": {
    "type": "genesis:orbiting",
    "parentID": "genesis:sun",
    "seed": 2,
    "orbitDistance": 15000,
    "orbitTime": 4600000,
    "dayLength": 24000
  },
  "properties": {
    "atmosphere": {
      "density": 1.0,
      "thickness": 1.0,
      "precipitation": true,
      "isBreathable": true,
      "color": { "type": "genesis:overworld" }
    }
  }
}
```

`data/genesis/genesis/celestials/moon.json`
```json
{
  "type": "genesis:body",
  "size": 29,
  "gravity": 0.1622,
  "transformProvider": {
    "type": "genesis:orbiting",
    "parentID": "minecraft:overworld",
    "seed": 67890,
    "orbitDistance": 750,
    "orbitTime": 184329,
    "dayLength": 0
  },
  "properties": {
    "atmosphere": {
      "density": 0.0,
      "thickness": 0.0,
      "precipitation": false,
      "isBreathable": false,
      "color": { "type": "genesis:rgb", "r": 5, "g": 5, "b": 10 }
    }
  }
}
```

`data/genesis/genesis/celestials/testbh.json`
```json
{
  "type": "genesis:blackhole",
  "size": 2048,
  "gravity": 8,
  "transformProvider": {
    "type": "genesis:static",
    "x": 0.0,
    "y": -200000.0,
    "z": -200000.0
  },
  "properties": {}
}
```
