package g_mungus.genesis.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
        int baseRadius = 8 + Math.min(rand.nextInt(20), rand.nextInt(25)); // 8–13 block radius

        // Random axis scaling: squash or stretch
        double scaleX = 1.0 + (rand.nextDouble() * 2.0 - 0.5); // 0.7 – 1.3
        double scaleY = 1.0 + (rand.nextDouble() * 2.0 - 0.5);
        double scaleZ = 1.0 + (rand.nextDouble() * 2.0 - 0.5);

        double noiseScale = 0.02;
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

                    double threshold = baseRadius - dist + noise * 6;

                    if (threshold > 1.5) {
                        level.setBlock(current, Blocks.STONE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
