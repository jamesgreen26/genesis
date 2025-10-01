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
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.extension.FallingBlockEntityExtension;
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

    public static Vector3d getRotation(int variant) {
        return switch (variant) {
            case 0 -> new Vector3d();
            case 1 -> new Vector3d(0, Math.toRadians(22.5), 0);
            case 2 -> new Vector3d(0, Math.toRadians(45), 0);
            case 3 -> new Vector3d(0, Math.toRadians(-22.5), 0);
            case 4 -> new Vector3d(Math.toRadians(22.5), 0, 0);
            case 5 -> new Vector3d(Math.toRadians(45), 0, 0);
            case 6 -> new Vector3d(Math.toRadians(-22.5), 0, 0);
            case 7 -> new Vector3d(0, 0, Math.toRadians(22.5));
            case 8 -> new Vector3d(0, 0, Math.toRadians(45));
            case 9 -> new Vector3d(0, 0, Math.toRadians(-22.5));
            default -> new Vector3d(0, 0, 0);
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            spawnFallingBlocks(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static Vector3d rotatePoint(double x, double y, double z, Vector3d eulerAngles) {
        double cosX = Math.cos(eulerAngles.x);
        double sinX = Math.sin(eulerAngles.x);
        double cosY = Math.cos(eulerAngles.y);
        double sinY = Math.sin(eulerAngles.y);
        double cosZ = Math.cos(eulerAngles.z);
        double sinZ = Math.sin(eulerAngles.z);

        double y1 = y * cosX - z * sinX;
        double z1 = y * sinX + z * cosX;

        double x2 = x * cosY + z1 * sinY;
        double z2 = -x * sinY + z1 * cosY;

        double x3 = x2 * cosZ - y1 * sinZ;
        double y3 = x2 * sinZ + y1 * cosZ;

        return new Vector3d(x3, y3, z2);
    }

    private void spawnFallingBlocks(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int variant = state.getValue(VARIANT);
        Vector3d rotation = getRotation(variant);

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

                    double relX = (x + 0.5) - 8.0;
                    double relY = (y + 0.5) - 8.0;
                    double relZ = (z + 0.5) - 8.0;

                    Vector3d rotated = rotatePoint(relX, relY, relZ, rotation);

                    double offsetX = (rotated.x + 8.0) / 16.0;
                    double offsetY = (rotated.y + 8.0) / 16.0;
                    double offsetZ = (rotated.z + 8.0) / 16.0;

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

                    double dirX = rotated.x;
                    double dirY = rotated.y;
                    double dirZ = rotated.z;

                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    double driftStrength = 0.25;

                    if (length > 0) {
                        dirX = (dirX / length) * driftStrength;
                        dirY = (dirY / length) * driftStrength;
                        dirZ = (dirZ / length) * driftStrength;
                    }

                    fallingBlock.setDeltaMovement(dirX, dirY, dirZ);

                    ((FallingBlockEntityExtension) fallingBlock).genesis$setRotation(new Vector3d(rotation));
                    fallingBlock.dropItem = false;

                    fallingBlock.time = 400 + level.random.nextInt(0, 100);

                    serverLevel.addFreshEntity(fallingBlock);
                }
            }
        }
    }
}
