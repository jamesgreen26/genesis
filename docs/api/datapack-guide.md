# Datapack Guide: Adding Celestials with JSON

This guide covers how to add celestials to Genesis using datapacks. Datapacks are the easiest way to customize your space environment without writing any code.

## File Structure

Celestial configurations are stored in JSON files within your datapack's `system_config` directory:

```
data/
└── <namespace>/
    └── system_config/
        └── <filename>.json
```

For example:
- `data/genesis/system_config/builtin.json` (Genesis default system)
- `data/mymod/system_config/solar_system.json` (Your custom system)

You can have multiple JSON files - all celestials from all files will be loaded and registered.

## JSON Format

Each configuration file contains a list of celestials:

```json
{
  "celestials": [
    {
      "ID": "namespace:celestial_id",
      "type": "namespace:type_id",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "provider_type",
        ...
      }
    }
  ]
}
```

## Step-by-Step Examples

### Example 1: A Simple Star

Let's create a star at the center of our solar system:

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    }
  ]
}
```

**Explanation:**
- `ID`: Unique identifier for this celestial
- `type`: Using built-in star type (emits light)
- `size`: 15x the base size (96 blocks), making it 1440 blocks diameter
- `gravity`: Standard gravity value
- `transformProvider`: Static position at coordinates (0, 0, 0)

### Example 2: An Orbiting Planet

Now let's add a planet that orbits our sun:

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "mymod:earth",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    }
  ]
}
```

**Explanation:**
- The planet uses `type: "genesis:body"` (solid, visitable)
- `transformProvider` is now `orbiting` type
- `parentID`: References our sun - the planet will orbit around it
- `seed`: Determines the random orbital angles (same seed = same orbit)
- `orbitDistance`: 1.0x the base distance (15,000 blocks from parent)
- `orbitTime`: 1.0x the base orbital period (4,608,000 ticks ≈ 64 hours)

### Example 3: Adding a Moon

Let's add a moon that orbits our planet:

```json
{
  "celestials": [
    {
      "ID": "mymod:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "mymod:earth",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    },
    {
      "ID": "mymod:moon",
      "type": "genesis:body",
      "size": 0.3,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "mymod:earth",
        "seed": 67890,
        "orbitDistance": 0.05,
        "orbitTime": 0.005,
        "dayLength": 0.75
      }
    }
  ]
}
```

**Explanation:**
- Moon is smaller (`size: 0.3`) and has lower gravity (`0.1622` ≈ real moon gravity)
- Moon orbits the planet (not the sun) via `parentID: "mymod:earth"`
- Much closer orbit (`orbitDistance: 0.05`) and faster (`orbitTime: 0.005`)
- `dayLength: 0.75` makes the moon rotate slower (0.75x normal day length)

## Property Reference

### Celestial Properties

#### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `ID` | String | Unique identifier (format: `namespace:name`) |
| `type` | String | Celestial type ID (e.g., `genesis:star`, `genesis:body`) |
| `size` | Number | Size multiplier (base size is 96 blocks diameter) |
| `gravity` | Number | Gravity strength multiplier (1.0 = Earth gravity) |
| `transformProvider` | Object | Position and rotation configuration (see below) |

#### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `r` | Number | 0.8 | Red color component (0.0 to 1.0) |
| `g` | Number | 0.8 | Green color component (0.0 to 1.0) |
| `b` | Number | 0.8 | Blue color component (0.0 to 1.0) |

### Static Transform Provider

Use this for celestials that don't move (like a sun at the center).

```json
"transformProvider": {
  "type": "genesis:static",
  "x": 0.0,
  "y": 0.0,
  "z": 0.0,
  "xRot": 0.0,
  "yRot": 0.0,
  "zRot": 0.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | String | - | Must be `"genesis:static"` |
| `x` | Number | - | X coordinate |
| `y` | Number | - | Y coordinate |
| `z` | Number | - | Z coordinate |
| `xRot` | Number | 0.0 | Rotation around X-axis (radians) |
| `yRot` | Number | 0.0 | Rotation around Y-axis (radians) |
| `zRot` | Number | 0.0 | Rotation around Z-axis (radians) |

### Orbiting Transform Provider

Use this for celestials that orbit around another celestial.

```json
"transformProvider": {
  "type": "genesis:orbiting",
  "parentID": "namespace:parent_celestial",
  "seed": 12345,
  "orbitDistance": 1.0,
  "orbitTime": 1.0,
  "dayLength": 1.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | String | - | Must be `"genesis:orbiting"` |
| `parentID` | String | - | ID of the celestial to orbit around |
| `seed` | Integer | - | Random seed for orbital angles and rotation |
| `orbitDistance` | Number | - | Orbit distance multiplier (base: 15,000 blocks) |
| `orbitTime` | Number | - | Orbital period multiplier (base: 4,608,000 ticks) |
| `dayLength` | Number | 1.0 | Day length multiplier (base: 24,000 ticks) |

**Understanding the multipliers:**
- `orbitDistance: 1.0` = 15,000 blocks from parent
- `orbitDistance: 0.5` = 7,500 blocks from parent
- `orbitTime: 1.0` = 4,608,000 ticks (64 hours) per orbit
- `orbitTime: 0.5` = 2,304,000 ticks (32 hours) per orbit
- `dayLength: 1.0` = 24,000 ticks (20 minutes) per rotation
- `dayLength: 2.0` = 48,000 ticks (40 minutes) per rotation

## Genesis Built-in Example

Here's the complete default solar system from Genesis (`builtin.json`):

```json
{
  "celestials": [
    {
      "ID": "genesis:sun",
      "type": "genesis:star",
      "size": 15.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:static",
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }
    },
    {
      "ID": "minecraft:overworld",
      "type": "genesis:body",
      "size": 1.0,
      "gravity": 1.0,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "genesis:sun",
        "seed": 12345,
        "orbitDistance": 1.0,
        "orbitTime": 1.0
      }
    },
    {
      "ID": "genesis:moon",
      "type": "genesis:body",
      "size": 0.3,
      "gravity": 0.1622,
      "transformProvider": {
        "type": "genesis:orbiting",
        "parentID": "minecraft:overworld",
        "seed": 67890,
        "orbitDistance": 0.05,
        "orbitTime": 0.005,
        "dayLength": 0.75
      }
    }
  ]
}
```

This creates:
- A large sun at the origin
- The Overworld orbiting the sun at standard distance and period
- A small moon orbiting the Overworld with lower gravity

## Textures

Custom textures can be added for celestial bodies using the `genesis:body` type (planets and moons). The Planet renderer automatically looks for textures in a specific location based on the celestial's ID.

**Important:** Textures are client-side assets and must be added via a **resource pack** or **mod** (not a datapack). Your datapack provides the celestial configuration (JSON), while textures are provided separately through client-side resources.

### Texture Location

Place PNG texture files in a resource pack or mod:

```
assets/<namespace>/textures/planets/<namespace>/<body_name>.png
```

**Examples (in a resource pack or mod):**
- `assets/genesis/textures/planets/minecraft/overworld.png`
- `assets/genesis/textures/planets/genesis/moon.png`
- `assets/mymod/textures/planets/mymod/custom_planet.png`

The texture path is automatically derived from the body's ID. For a celestial with ID `mymod:custom_planet`, the renderer looks for:

```
assets/genesis/textures/planets/mymod/custom_planet.png
```

**Workflow:** Define your celestial in a datapack JSON (server-side), then add matching textures in a resource pack (client-side). If no texture is found, the body will be rendered using the RGB color values specified in the configuration.

### Texture Layout

Planet and moon textures use a **cube map layout** arranged in a **3×2 grid**. The texture should be divided into 6 equal sections representing the faces of a cube:

**Top Row (upper half of texture):**
- Left third: **North face**
- Middle third: **West face**
- Right third: **South face**

**Bottom Row (lower half of texture):**
- Left third: **East face**
- Middle third: **Down face** (bottom of the planet)
- Right third: **Up face** (top of the planet)

```
┌─────────┬─────────┬─────────┐
│  North  │  West   │  South  │  ← Top row
├─────────┼─────────┼─────────┤
│  East   │  Down   │   Up    │  ← Bottom row
└─────────┴─────────┴─────────┘
```

### Texture Guidelines

When creating your texture:

- **Dimensions**: Should be divisible by 3 horizontally and by 2 vertically for clean mapping
- **Edge matching**: Adjacent faces should have matching edges where they meet on the cube
- **Orientation**: The Up and Down faces represent the top and bottom of the planet; the four cardinal faces (North, East, South, West) wrap around the sides
- **Recommended sizes**: Use powers of 2 for best results (e.g., 192×128, 384×256, 768×512) to ensure each face section divides evenly

## Next Steps

- **Want to register celestials in code?** See the [Code Guide](code-guide.md)
- **Need custom celestial types or orbit patterns?** Check out [Advanced Topics](advanced.md)
- **Looking for detailed API information?** Browse the [API Reference](api-reference.md)
