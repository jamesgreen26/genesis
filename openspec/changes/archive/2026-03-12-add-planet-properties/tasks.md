## 1. Codecs

- [x] 1.1 Add `PlanetColorPalette` dispatch codec with `genesis:rgb` and `genesis:overworld` variants
- [x] 1.2 Add `PlanetProperties` Mojang codec (fields: `id`, `density`, `thickness`, `precipitation`, `color`)
- [x] 1.3 Add `PlanetPropertiesModel` record (wraps a `List<PlanetPropertiesEntry>`) with codec, mirroring `SystemConfigModel`

## 2. Registry

- [x] 2.1 Make `PlanetProperties.PLANET_PROPERTIES` package-accessible with a `register(ResourceLocation, PlanetProperties)` helper
- [x] 2.2 Add `PlanetProperties.reset()` to clear the map
- [x] 2.3 Expose `PlanetProperties.get(ResourceLocation)` as public API (already exists, ensure it's public)

## 3. Data Loading (`planet_properties/DataLoader`)

- [x] 3.1 Implement `DataLoader` as `@Mod.EventBusSubscriber` with `AddReloadListenerEvent` handler
- [x] 3.2 Scan `system_config/planet_properties/` via `ResourceManager.listResources`
- [x] 3.3 Parse each file into `PlanetPropertiesModel`, populate `PlanetProperties.PLANET_PROPERTIES`
- [x] 3.4 Call `PlanetPropertiesSyncPacket.sendToAllClients()` after loading completes

## 4. Client Sync (`PlanetPropertiesSyncPacket`)

- [x] 4.1 Implement encode/decode using NBT (same pattern as `SpaceRegistrySyncPacket`)
- [x] 4.2 Implement `sendToAllClients()` using `GenesisNetworking`
- [x] 4.3 Add `PlayerLoggedInEvent` handler to send packet to newly joining players
- [x] 4.4 Implement client-side `handle()` to clear and repopulate `PLANET_PROPERTIES`
- [x] 4.5 Register packet in `GenesisNetworking`
