# Adding Celestial Bodies to Genesis

Genesis allows you to add custom stars and planets (orbiting bodies) to the solar system through JSON configuration files or programmatically through code.

## JSON Configuration

### File Location

Create JSON files in the `data/<namespace>/system_config/` directory of your datapack or mod resources.

For example: `data/genesis/system_config/builtin.json`

### Configuration Structure

```json
{
  "stars": [
    {
      "ID": "genesis:sun",
      "temperature": 1.0,
      "size": 15.0,
      "x": 0.0,
      "y": 0.0,
      "z": 0.0
    }
  ],
  "bodies": [
    {
      "ID": "minecraft:overworld",
      "parentID": "genesis:sun",
      "size": 1.0,
      "orbitDistance": 1.0,
      "orbitTime": 1.0,
      "gravity": 1.0
    },
    {
      "ID": "genesis:moon",
      "parentID": "minecraft:overworld",
      "size": 0.3,
      "orbitDistance": 0.05,
      "orbitTime": 0.005,
      "gravity": 0.1622,
      "r": 0.8,
      "g": 0.8,
      "b": 0.8
    }
  ]
}
```

## Star Properties

Stars are fixed-position celestial objects that emit light.

| Property | Type | Required | Description                                                          |
|----------|------|----------|----------------------------------------------------------------------|
| `ID` | String | Yes | Resource location identifier for the star                            |
| `temperature` | Number | Yes | Temperature of the star (affects color/brightness, 1.0 = Sun)        |
| `size` | Number | Yes | Visual size of the star (1.0 = Earth size, 15.0 = Sun size) |
| `x` | Number | Yes | X position in space                                                  |
| `y` | Number | Yes | Y position in space                                                  |
| `z` | Number | Yes | Z position in space                                                  |

## Orbiting Body Properties

Orbiting bodies are planets or moons that orbit around a parent star or planet.

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `ID` | String | Yes | **Dimension ID** - The ResourceLocation of the dimension this body represents |
| `parentID` | String | Yes | ID of the parent celestial body (star or planet) to orbit around |
| `size` | Number | Yes | Visual size of the body (1.0 = Earth scale) |
| `orbitDistance` | Number | Yes | Distance from the parent body (1.0 = Earth's distance from Sun) |
| `orbitTime` | Number | Yes | Time to complete one orbit (1.0 = Earth year) |
| `gravity` | Number | Yes | Gravitational strength (1.0 = Earth gravity, 0.1622 = Moon gravity) |
| `r` | Number | No | Red color component (0.0-1.0). Optional fallback if no texture is provided. Default: 0.5 |
| `g` | Number | No | Green color component (0.0-1.0). Optional fallback if no texture is provided. Default: 0.5 |
| `b` | Number | No | Blue color component (0.0-1.0). Optional fallback if no texture is provided. Default: 0.5 |
| `customTransform` | Object | No | Custom transform provider for advanced orbital mechanics. See [CustomTransformProvider API](CustomTransformProvider_API.md) |

### Scale Reference

All orbital mechanics values use Earth as the baseline (1.0):

- **size**: 1.0 = Earth diameter
- **orbitDistance**: 1.0 = 1 AU (Earth-Sun distance)
- **orbitTime**: 1.0 = 1 Earth year (365.25 days)
- **gravity**: 1.0 = Earth gravity (9.8 m/s²)

### Important Notes

- The `ID` field for orbiting bodies **must match** the dimension ID of the Minecraft dimension it represents
- The RGB color values are **optional** and only used as a fallback when no texture is provided

## Textures

To add custom textures for your celestial bodies, place PNG texture files in:

```
assets/<namespace>/textures/planets/<namespace>/<body_name>.png
```

For example:
- `assets/genesis/textures/planets/minecraft/overworld.png`
- `assets/genesis/textures/planets/genesis/moon.png`

The texture path is automatically derived from the body's ID. For a body with ID `mymod:custom_planet`, the texture should be at:
```
assets/genesis/textures/planets/mymod/custom_planet.png
```

If no texture is found, the body will be rendered using the RGB color values specified in the configuration.

## Texture Layout

Planet and moon textures use a cube map layout arranged in a 3x2 grid. The texture should be divided into 6 equal sections representing the faces of a cube:

**Top Row (upper half of texture):**
- **Left third**: North face
- **Middle third**: West face
- **Right third**: South face

**Bottom Row (lower half of texture):**
- **Left third**: East face
- **Middle third**: Down face (bottom of the planet)
- **Right third**: Up face (top of the planet)

When creating your texture, ensure that:

1. The texture dimensions are divisible by 3 horizontally and by 2 vertically for clean mapping
2. Adjacent faces should have matching edges where they meet on the cube
3. The Up and Down faces represent the top and bottom of the planet
4. The four cardinal faces (North, East, South, West) wrap around the sides

For best results, use textures with dimensions that are powers of 2 (e.g., 192x128, 384x256, 768x512) to ensure each face section divides evenly.

## Programmatic Registration

You can also register celestial bodies through code using the registration callback:

```java
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Star;
import shipwrights.genesis.space.OrbitingBody;

public class MyMod {
    public static void init() {
        GenesisMod.onRegisterCelestialsEvent(registerEvent -> {
            // Register a star
            Star customStar = new Star(
                "mymod:custom_star",  // ID
                1.2,                  // temperature
                20.0,                 // size
                100000.0,             // x
                50000.0,              // y
                0.0                   // z
            );
            registerEvent.accept(customStar.getID(), customStar);

            // Register a planet
            OrbitingBody customPlanet = new OrbitingBody(
                "mymod:custom_dimension",  // dimension ID
                "mymod:custom_star",       // parent ID
                1.5,                       // size (1.5x Earth)
                1.2,                       // orbit distance (1.2 AU)
                2.0,                       // orbit time (2 Earth years)
                1.1,                       // gravity (1.1g)
                0.7f,                      // red
                0.5f,                      // green
                0.3f                       // blue
            );
            registerEvent.accept(customPlanet.getID(), customPlanet);
        });
    }
}
```

## Examples

### Creating Mars

```json
{
  "bodies": [
    {
      "ID": "mymod:mars",
      "parentID": "genesis:sun",
      "size": 0.532,
      "orbitDistance": 1.524,
      "orbitTime": 1.881,
      "gravity": 0.38,
      "r": 0.8,
      "g": 0.4,
      "b": 0.3
    }
  ]
}
```

### Creating a Moon System

```json
{
  "bodies": [
    {
      "ID": "mymod:gas_giant",
      "parentID": "genesis:sun",
      "size": 11.0,
      "orbitDistance": 5.2,
      "orbitTime": 11.86,
      "gravity": 2.5
    },
    {
      "ID": "mymod:moon_1",
      "parentID": "mymod:gas_giant",
      "size": 0.4,
      "orbitDistance": 0.02,
      "orbitTime": 0.01,
      "gravity": 0.15
    },
    {
      "ID": "mymod:moon_2",
      "parentID": "mymod:gas_giant",
      "size": 0.3,
      "orbitDistance": 0.03,
      "orbitTime": 0.02,
      "gravity": 0.12
    }
  ]
}
```
