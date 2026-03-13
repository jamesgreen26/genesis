# Proposal: Planet Properties

## Why

Planets (`genesis:body`) are the only visitable celestial type, meaning players actually land on and inhabit them. The base `Celestial` model is intentionally minimal — just enough to position, render, and orbit a body. But planets need richer environmental data: atmospheric density, atmospheric thickness, whether it rains, and what color the sky/fog should appear.

Adding this data directly to `Celestial` would bloat every celestial type with planet-specific fields. Instead, planet properties live in a separate, parallel registry loaded from `system_config/planet_properties/` — the same resource pack directory as celestial configs, but a distinct sub-path so the two loaders don't conflict.

## What Changes

- New `PlanetProperties` record keyed by celestial `ResourceLocation`, holding `density`, `thickness`, `precipitation`, and a `PlanetColorPalette`
- `PlanetColorPalette` is a polymorphic type with two variants: explicit `RGB` and a special `Overworld` sentinel that defers to actual biome sky colors
- New `planet_properties/DataLoader` loads JSON files from `system_config/planet_properties/` using the same reload-listener pattern as the space registry
- New `PlanetPropertiesSyncPacket` mirrors `SpaceRegistrySyncPacket`: sends the full properties map to clients on bake and on player join
- The existing `system_config` celestial loader already excludes `planet_properties/` paths to avoid parse conflicts

## Impact

- Resource pack authors can add `system_config/planet_properties/<id>.json` files to define planet environments without touching celestial definitions
- Planet properties are client-synced, so sky/atmosphere rendering on the client always reflects server-loaded data
- No changes to `Celestial`, `SpaceRegistry`, or any existing celestial data files
