package g_mungus.genesis.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import open_simplex_2.java.OpenSimplex2;

import java.util.Random;

public class TestingStickItem extends Item {

    public TestingStickItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos().above();
        long seed = serverLevel.getSeed() ^ clickedPos.asLong();
        generateAsteroid(serverLevel, clickedPos, seed);

        return InteractionResult.SUCCESS;
    }

    private void generateAsteroid(ServerLevel level, BlockPos center, long seed) {
        Random rand = new Random(seed);

        // Vary base size and shape per asteroid
        int baseRadius = 5; // 8–13 block radius

        // Random axis scaling: squash or stretch
        double scaleX = 1.0 + (rand.nextDouble() * 0.5);
        double scaleY = 1.0 + (rand.nextDouble() * 0.5);
        double scaleZ = 1.0 + (rand.nextDouble() * 0.5);

        double noiseScale = 0.04;
        Vec3 centerVec = new Vec3(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);

        int xRadius = (int) Math.ceil(baseRadius * scaleX * 1.5);
        int yRadius = (int) Math.ceil(baseRadius * scaleY * 1.5);
        int zRadius = (int) Math.ceil(baseRadius * scaleZ * 1.5);

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dy = -yRadius; dy <= yRadius; dy++) {
                for (int dz = -zRadius; dz <= zRadius; dz++) {
                    BlockPos current = center.offset(dx, dy, dz);
                    Vec3 posVec = new Vec3(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);

                    // Apply inverse scaling to get into "spherical blob space"
                    double x = (posVec.x - centerVec.x) / scaleX;
                    double y = (posVec.y - centerVec.y) / scaleY;
                    double z = (posVec.z - centerVec.z) / scaleZ;

                    double dist = Math.sqrt(x * x + y * y + z * z);

                    // Multi-layer OpenSimplex2 noise
                    double n1 = OpenSimplex2.noise3_ImproveXZ(seed, posVec.x * noiseScale, posVec.y * noiseScale, posVec.z * noiseScale);
                    double n2 = OpenSimplex2.noise3_ImproveXZ(seed + 1, posVec.x * noiseScale * 2, posVec.y * noiseScale * 2, posVec.z * noiseScale * 2);
                    double n3 = OpenSimplex2.noise3_ImproveXZ(seed + 2, posVec.x * noiseScale * 4, posVec.y * noiseScale * 4, posVec.z * noiseScale * 4);

                    double noise = (n1 * 0.6 + n2 * 0.3 + n3 * 0.1);

                    double threshold = baseRadius - dist + noise * 4;

                    if (threshold > 1.5) {
                        level.setBlock(current, getAsteroidBlock(current.getCenter(), seed), 3);
                    }
                }
            }
        }
    }

    private BlockState getAsteroidBlock(Vec3 pos, long seed) {
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
