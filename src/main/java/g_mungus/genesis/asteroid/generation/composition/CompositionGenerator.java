package g_mungus.genesis.asteroid.generation.composition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import open_simplex_2.java.OpenSimplex2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;
import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.generateAsteroid;

public class CompositionGenerator {

    public static void generateAndSaveAll() {
        int numberOfThreads = 12;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = ASTEROID_COUNT; i >= 0; i--) {
            final int taskId = i;
            executor.submit(() -> generateAndSave(taskId));
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

    public static void generateAndSave(long seed) {
        List<BlockPos> rawShape = generateAsteroid(seed);
        long actualSeed = seed;
        while (rawShape.size() > 50_000) {
            System.out.println("skipping asteroid " + seed + " with size: " + rawShape.size());
            actualSeed *= seed;
            rawShape = generateAsteroid(actualSeed);
        }

        Map<String, Integer> blockCounts = new HashMap<>();

        for (BlockPos pos : rawShape) {
            BlockState state = getAsteroidBlock(new Vec3(pos.getX(), pos.getY(), pos.getZ()), seed);
            String blockId = blockStateToId(state);

            blockCounts.merge(blockId, 1, Integer::sum);
        }

        List<CompositionData.BlockComposition> composition = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            CompositionData.BlockComposition comp = new CompositionData.BlockComposition();
            comp.setType(entry.getKey());
            comp.setAmount(entry.getValue());

            composition.add(comp);
        }

        CompositionData toSave = new CompositionData();
        toSave.setComposition(composition);
        saveComposition(toSave, seed);
    }

    static void saveComposition(CompositionData compositionData, long seed) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path outputDir = gameDir.resolve("genesis_mod/composition");
        Path outputFile = outputDir.resolve("asteroid_" + seed + ".json");

        try {
            Files.createDirectories(outputDir);

            Files.writeString(outputFile, compositionData.toJson());

            System.out.println("Composition written to: " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String blockStateToId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static BlockState getAsteroidBlock(Vec3 pos, long seed) {
        Random rand = new Random(seed + 843);

        BlockState[] baseMaterials = new BlockState[] {
                Blocks.STONE.defaultBlockState(),
                Blocks.ANDESITE.defaultBlockState(),
                Blocks.DEEPSLATE.defaultBlockState(),
                Blocks.BASALT.defaultBlockState()
        };
        BlockState baseBlock = baseMaterials[rand.nextInt(baseMaterials.length)];


        // Low-frequency noise for large clumps
        double oreNoiseScale = 0.05;

        double nx = pos.x * oreNoiseScale;
        double ny = pos.y * oreNoiseScale;
        double nz = pos.z * oreNoiseScale;

        double clusterNoise = OpenSimplex2.noise3_ImproveXZ(seed + 100, nx, ny, nz);

        double ironNoise = OpenSimplex2.noise3_ImproveXZ(seed + 101, nx, ny, nz);
        double coalNoise = OpenSimplex2.noise3_ImproveXZ(seed + 102, nx, ny, nz);
        double redstoneNoise = OpenSimplex2.noise3_ImproveXZ(seed + 103, nx, ny, nz);
        double copperNoise = OpenSimplex2.noise3_ImproveXZ(seed + 104, nx, ny, nz);

        if (baseBlock.is(Blocks.DEEPSLATE) || baseBlock.is(Blocks.BASALT)) {
            if (ironNoise > 0.5 && clusterNoise > 0.2) return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
            if (coalNoise > 0.45 && clusterNoise > 0.1) return Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
            if (redstoneNoise > 0.6 && clusterNoise > 0.25) return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
            if (copperNoise > 0.7 && clusterNoise > 0.4) return Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState();
        } else {
            if (ironNoise > 0.5 && clusterNoise > 0.2) return Blocks.IRON_ORE.defaultBlockState();
            if (coalNoise > 0.45 && clusterNoise > 0.1) return Blocks.COAL_ORE.defaultBlockState();
            if (redstoneNoise > 0.6 && clusterNoise > 0.25) return Blocks.REDSTONE_ORE.defaultBlockState();
            if (copperNoise > 0.7 && clusterNoise > 0.4) return Blocks.COPPER_ORE.defaultBlockState();
        }

        return baseBlock;
    }
}
