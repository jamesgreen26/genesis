package g_mungus.genesis.asteroid.generation.composition;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import open_simplex_2.java.OpenSimplex2;

import java.util.Random;

public class CompositionGenerator {

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
