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
        int radius = 10;
        double scale = 0.1;

        Vec3 centerVec = new Vec3(center.getX(), center.getY(), center.getZ());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos current = center.offset(dx, dy, dz);
                    Vec3 currentVec = new Vec3(current.getX(), current.getY(), current.getZ());

                    double distance = centerVec.distanceTo(currentVec);
                    double noiseValue = OpenSimplex2.noise3_ImproveXZ(seed, current.getX() * scale, current.getY() * scale, current.getZ() * scale);

                    // Threshold based on distance and noise
                    if (distance + noiseValue * 6 < radius) {
                        level.setBlock(current, Blocks.STONE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
