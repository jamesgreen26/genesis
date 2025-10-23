package shipwrights.dataplanets.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RegistryUtil {

    public static void registerBiome(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            Biome biome
    ) {
        registerThing(server, Registries.BIOME, ResourceKey.create(Registries.BIOME, resourceLocation), biome);
        writeToDatapack(server, resourceLocation, "worldgen/biome", Biome.DIRECT_CODEC, biome);
    }

    public static void registerDimensionType(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            DimensionType dimensionType
    ) {
        registerThing(server, Registries.DIMENSION_TYPE, ResourceKey.create(Registries.DIMENSION_TYPE, resourceLocation), dimensionType);
        writeToDatapack(server, resourceLocation, "dimension_type", DimensionType.DIRECT_CODEC, dimensionType);
    }

    public static void registerLevelStem(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            LevelStem levelStem
    ) {
        registerThing(server, Registries.LEVEL_STEM, ResourceKey.create(Registries.LEVEL_STEM, resourceLocation), levelStem);
        writeToDatapack(server, resourceLocation, "dimension", LevelStem.CODEC, levelStem);
    }

    public static void registerConfiguredCarver(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredWorldCarver<?> configuredCarver
    ) {
        registerThing(server, Registries.CONFIGURED_CARVER, ResourceKey.create(Registries.CONFIGURED_CARVER, resourceLocation), configuredCarver);
        //TODO write to datapack

    }

    public static void registerPlacedFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            PlacedFeature placedFeature
    ) {
        registerThing(server, Registries.PLACED_FEATURE, ResourceKey.create(Registries.PLACED_FEATURE, resourceLocation), placedFeature);
        //TODO write to datapack

    }

    public static void registerConfiguredFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredFeature<?, ?> configuredFeature
    ) {
        registerThing(server, Registries.CONFIGURED_FEATURE, ResourceKey.create(Registries.CONFIGURED_FEATURE, resourceLocation), configuredFeature);
        //TODO write to datapack

    }

    public static void registerNoise(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            NormalNoise.NoiseParameters noiseParameters
    ) {
        registerThing(server, Registries.NOISE, ResourceKey.create(Registries.NOISE, resourceLocation), noiseParameters);
        //TODO write to datapack

    }

    public static void registerNoiseSettings(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            NoiseGeneratorSettings noiseGeneratorSettings
    ) {
        registerThing(server, Registries.NOISE_SETTINGS, ResourceKey.create(Registries.NOISE_SETTINGS, resourceLocation), noiseGeneratorSettings);
        writeToDatapack(server, resourceLocation, "worldgen/noise_settings", NoiseGeneratorSettings.DIRECT_CODEC, noiseGeneratorSettings);
    }

    @SuppressWarnings("deprecation")
    private static <T> void registerThing(
            MinecraftServer server,
            ResourceKey<Registry<T>> registry,
            ResourceKey<T> keyToRegister,
            T thingToRegister
    ) {
        Registry<T> reg = server.registryAccess().registryOrThrow(registry);
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.unfreeze();
            mapped.register(keyToRegister, thingToRegister, Lifecycle.experimental());
            mapped.freeze();
        } else {
            throw new IllegalArgumentException("Cannot modify registry: " + registry.registry());
        }
    }

    public static boolean setupDatapackFolder(MinecraftServer server) {
        Path basePath = server.storageSource.getLevelPath(LevelResource.DATAPACK_DIR);
        Path dataplanetsFolder = basePath.resolve("dataplanets-generated");

        String mcMeta = "{\n" +
                "  \"pack\": {\n" +
                "    \"description\": \"Dataplanets Generated\",\n" +
                "    \"forge:server_data_pack_format\": 15,\n" +
                "    \"pack_format\": 15\n" +
                "  }\n" +
                "}";

        boolean newFolderCreated = false;

        try {
            if (!Files.exists(dataplanetsFolder)) {
                Files.createDirectories(dataplanetsFolder);
                newFolderCreated = true;
            }

            Path mcMetaPath = dataplanetsFolder.resolve("pack.mcmeta");
            if (!Files.exists(mcMetaPath)) {
                Files.writeString(mcMetaPath, mcMeta);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dataplanets-generated folder", e);
        }

        return newFolderCreated;
    }

    private static <T> void writeToDatapack(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            String path,
            com.mojang.serialization.Codec<T> codec,
            T object
    ) {
        Path basePath = server.storageSource.getLevelPath(LevelResource.DATAPACK_DIR);
        Path dataplanetsFolder = basePath.resolve("dataplanets-generated");
        Path typeFolder = dataplanetsFolder.resolve("data")
                .resolve(resourceLocation.getNamespace())
                .resolve(path);
        Path objectFile = typeFolder.resolve(resourceLocation.getPath() + ".json");

        try {
            Files.createDirectories(typeFolder);

            // Use RegistryOps to serialize with registry references instead of inlining
            RegistryAccess registryAccess = server.registryAccess();
            RegistryOps<JsonElement> registryOps = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryAccess);

            com.mojang.serialization.DataResult<JsonElement> result = codec.encodeStart(
                    registryOps,
                    object
            );

            // Check if encoding was successful
            if (result.error().isPresent()) {
                System.err.println("Warning: Failed to encode " + path + " for " + resourceLocation + ": " + result.error().get().message());
                System.err.println("Skipping datapack file generation for this object.");
                return; // Skip writing this file
            }

            JsonElement json = result.result().orElseThrow();

            // Write to file with proper formatting
            String jsonString = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(json);

            Files.writeString(objectFile, jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + path + " file: " + resourceLocation, e);
        }
    }
}
