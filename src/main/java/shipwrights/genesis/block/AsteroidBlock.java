package shipwrights.genesis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import shipwrights.genesis.mixin.FallingBlockEntityAccessor;

public class AsteroidBlock extends Block {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 9);

    public AsteroidBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            spawnFallingBlocks(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void spawnFallingBlocks(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    boolean isOuterLayer = x < 2 || x >= 14 || y < 2 || y >= 14 || z < 2 || z >= 14;
                    if (!isOuterLayer) {
                        continue;
                    }

                    if (serverLevel.random.nextBoolean()) {
                        continue;
                    }

                    double offsetX = x / 16.0;
                    double offsetY = y / 16.0;
                    double offsetZ = z / 16.0;

                    BlockState blockState = switch (serverLevel.random.nextInt(4)) {
                        case 1 -> Blocks.ANDESITE.defaultBlockState();
                        case 2 -> Blocks.GRAVEL.defaultBlockState();
                        case 3 -> Blocks.COAL_ORE.defaultBlockState();
                        default -> Blocks.STONE.defaultBlockState();
                    };

                    FallingBlockEntity fallingBlock = FallingBlockEntityAccessor.invokeConstructor(
                            serverLevel,
                            pos.getX() + offsetX,
                            pos.getY() + offsetY,
                            pos.getZ() + offsetZ,
                            blockState
                    );

                    double centerX = 7.5;
                    double centerY = 7.5;
                    double centerZ = 7.5;

                    double dirX = (x + 0.5) - centerX;
                    double dirY = (y + 0.5) - centerY;
                    double dirZ = (z + 0.5) - centerZ;

                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    double driftStrength = 0.25;

                    if (length > 0) {
                        dirX = (dirX / length) * driftStrength;
                        dirY = (dirY / length) * driftStrength;
                        dirZ = (dirZ / length) * driftStrength;
                    }

                    fallingBlock.setDeltaMovement(dirX, dirY, dirZ);

                    fallingBlock.time = 500;

                    serverLevel.addFreshEntity(fallingBlock);
                }
            }
        }
    }
}
