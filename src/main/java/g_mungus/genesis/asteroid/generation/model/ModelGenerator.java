package g_mungus.genesis.asteroid.generation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import g_mungus.genesis.asteroid.generation.voxel_shape.bitset.BitSetDeserializer;
import g_mungus.genesis.asteroid.generation.voxel_shape.bitset.BitSetSerializer;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.loading.FMLPaths;
import org.joml.Vector3i;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;
import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.generateAsteroid;

public class ModelGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();


    public static void generateAndSaveAll() {
        SimpleModule module = new SimpleModule();

        module.addSerializer(BitSet.class, new BitSetSerializer());
        module.addDeserializer(BitSet.class, new BitSetDeserializer());
        mapper.registerModule(module);

        int numberOfThreads = 8;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = ASTEROID_COUNT; i >= 0; i--) {
            final int taskId = i;
            executor.submit(() -> generateAndSaveModel(taskId));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(12, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static void generateAndSaveModel(long seed) {
        List<BlockPos> rawShape = generateAsteroid(seed);
        long actualSeed = seed;
        while (rawShape.size() > 50_000) {
            System.out.println("skipping asteroid " + seed + " with size: " + rawShape.size());
            actualSeed *= seed;
            rawShape = generateAsteroid(actualSeed);
        }

        ModelVoxelSet modelVoxelSet = new ModelVoxelSet();
        rawShape.forEach(it -> {
            modelVoxelSet.voxels.put(new Vector3i(it.getX(), it.getY(), it.getZ()), new ModelVoxelSet.VoxelWithFaces());
        });

        modelVoxelSet.optimize();

        ObjectNode json = VoxelSetToJson.generateVoxelJson(modelVoxelSet);

        Path gameDir = FMLPaths.GAMEDIR.get();
        Path outputDir = gameDir.resolve("genesis_mod/model/");
        Path outputFile = outputDir.resolve("asteroid_" + seed + ".json");

        try {

            Files.createDirectories(outputDir);
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), json);
            System.out.println("Model written to: " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
