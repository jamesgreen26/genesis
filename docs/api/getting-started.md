# Getting Started with Celestials

## Introduction

Celestials are the stars, planets, moons, and other astronomical bodies in the Genesis mod. They form the foundation of your space environment, defining the positions, orbits, and properties of every celestial object in your universe.

This guide will introduce you to the core concepts of celestials and show you how to add them to your mod or datapack.

## Core Concepts

### What is a Celestial?

A celestial is any astronomical body in your space system. Each celestial has:

- **Identity**: A unique ID (like `genesis:sun` or `minecraft:overworld`)<br>    NOTE: If your celestial has an associated dimension, it should have the same ID
- **Type**: What kind of celestial it is (star, planet, etc.)
- **Physical Properties**: Size, gravity, and color
- **Transform**: Position and rotation behavior (static or orbiting)

### Celestial Types

Genesis includes two built-in celestial types:

- **`genesis:star`**: Light-emitting bodies like suns
  - Casts light on other celestials
  - Not visitable (players can't land on them)
  - Rendered with glowing effects

- **`genesis:body`**: Solid bodies like planets and moons
  - Don't emit light
  - Cast shadows
  - Visitable (players can land on them)
  - Rendered with surface textures

### Transform Providers

Transform providers control how a celestial moves and rotates. Genesis provides two built-in options:

- **Static Transform**: For celestials that stay in one place (like a sun at the center of a solar system)
- **Orbiting Transform**: For celestials that orbit around a parent body (like planets orbiting a sun)

### Physical Properties

Each celestial has several configurable properties:

- **Size**: Relative size multiplier (1.0 = 96 blocks diameter)
- **Gravity**: Gravitational pull strength (1.0 = Earth-like gravity)
- **Color**: RGB values (0.0 to 1.0) that tint the celestial's appearance

## Quick Start Example

Here's the simplest possible celestial - a static star at the origin:

```json
{
  "celestials": [
    {
      "ID": "mymod:my_sun",
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

This creates a large star named `mymod:my_sun` positioned at coordinates (0, 0, 0) that never moves.

## How to Add Celestials

You can add celestials to Genesis in two ways:

1. **Datapacks** (Recommended for most users)
   - Create JSON configuration files
   - No programming required
   - Easy to share and modify

2. **Code** (For mod developers, offers more control)
   - Register celestials programmatically in Java
   - Full programmatic control
   - Can create dynamic celestials

Both approaches are covered in the [Implementation Guide](implementation-guide.md), which explains each concept once and shows examples for both formats.

## Prerequisites

### For Datapacks
- A Minecraft instance with Genesis installed
- Basic understanding of JSON format
- A text editor

### For Code Registration
- Java development environment
- Genesis mod as a dependency
- Understanding of Minecraft mod development

## Next Steps

- **Ready to create celestials?** Start with the [Implementation Guide](implementation-guide.md) to learn how to create celestials using JSON datapacks or Java code
- **Want to go deeper?** Explore [Advanced Topics](advanced.md) for custom types and transform providers
- **Need a reference?** See the complete [API Reference](api-reference.md)

## Example: Genesis Default Solar System

Genesis comes with a default solar system configured in `builtin.json`:

- **Sun** (`genesis:sun`): A large star at the center
- **Overworld** (`minecraft:overworld`): Earth-like planet orbiting the sun
- **Moon** (`genesis:moon`): Small moon orbiting the Overworld

You can view this configuration as a reference example for building your own celestial systems.
