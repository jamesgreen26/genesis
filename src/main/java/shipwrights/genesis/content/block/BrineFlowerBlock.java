package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import shipwrights.genesis.content.GenesisTags;

import javax.annotation.Nullable;

public class BrineFlowerBlock extends Block {
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE;
    private final BrineTrunkPlantBlock plant;

    public BrineFlowerBlock(BrineTrunkPlantBlock arg, BlockBehaviour.Properties arg2) {
        super(arg2);
        this.plant = arg;
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0));
    }

    public void tick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        if (!arg.canSurvive(arg2, arg3)) {
            arg2.destroyBlock(arg3, true);
        }

    }

    public boolean isRandomlyTicking(BlockState arg) {
        return (Integer)arg.getValue(AGE) < 5;
    }

    public void randomTick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        BlockPos blockpos = arg3.above();
        if (arg2.isEmptyBlock(blockpos) && blockpos.getY() < arg2.getMaxBuildHeight()) {
            int i = (Integer)arg.getValue(AGE);
            if (i < 5 && ForgeHooks.onCropsGrowPre(arg2, blockpos, arg, true)) {
                boolean flag = false;
                boolean flag1 = false;
                BlockState blockstate = arg2.getBlockState(arg3.below());
                if (!blockstate.is(GenesisTags.Blocks.SALT_PLANTABLE)) {
                    if (!blockstate.is(this.plant)) {
                        if (blockstate.isAir()) {
                            flag = true;
                        }
                    } else {
                        int j = 1;

                        for(int k = 0; k < 4; ++k) {
                            BlockState blockstate1 = arg2.getBlockState(arg3.below(j + 1));
                            if (!blockstate1.is(this.plant)) {
                                if (blockstate1.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE)) {
                                    flag1 = true;
                                }
                                break;
                            }

                            ++j;
                        }

                        if (j < 2 || j <= arg4.nextInt(flag1 ? 5 : 4)) {
                            flag = true;
                        }
                    }
                } else {
                    flag = true;
                }

                if (flag && allNeighborsEmpty(arg2, blockpos, (Direction)null) && arg2.isEmptyBlock(arg3.above(2))) {
                    arg2.setBlock(arg3, this.plant.getStateForPlacement(arg2, arg3), 2);
                    this.placeGrownFlower(arg2, blockpos, i);
                } else if (i >= 4) {
                    this.placeDeadFlower(arg2, arg3);
                } else {
                    int l = arg4.nextInt(4);
                    if (flag1) {
                        ++l;
                    }

                    boolean flag2 = false;

                    for(int i1 = 0; i1 < l; ++i1) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(arg4);
                        BlockPos blockpos1 = arg3.relative(direction);
                        if (arg2.isEmptyBlock(blockpos1) && arg2.isEmptyBlock(blockpos1.below()) && allNeighborsEmpty(arg2, blockpos1, direction.getOpposite())) {
                            this.placeGrownFlower(arg2, blockpos1, i + 1);
                            flag2 = true;
                        }
                    }

                    if (flag2) {
                        arg2.setBlock(arg3, this.plant.getStateForPlacement(arg2, arg3), 2);
                    } else {
                        this.placeDeadFlower(arg2, arg3);
                    }
                }

                ForgeHooks.onCropsGrowPost(arg2, arg3, arg);
            }
        }

    }

    private void placeGrownFlower(Level arg, BlockPos arg2, int i) {
        arg.setBlock(arg2, (BlockState)this.defaultBlockState().setValue(AGE, i), 2);
        arg.levelEvent(1033, arg2, 0);
    }

    private void placeDeadFlower(Level arg, BlockPos arg2) {
        arg.setBlock(arg2, (BlockState)this.defaultBlockState().setValue(AGE, 5), 2);
        arg.levelEvent(1034, arg2, 0);
    }

    private static boolean allNeighborsEmpty(LevelReader arg, BlockPos arg2, @Nullable Direction arg3) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != arg3 && !arg.isEmptyBlock(arg2.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        if (arg2 != Direction.UP && !arg.canSurvive(arg4, arg5)) {
            arg4.scheduleTick(arg5, this, 1);
        }

        return super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        BlockState blockstate = arg2.getBlockState(arg3.below());
        if (!blockstate.is(this.plant) && !blockstate.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE)) {
            if (!blockstate.isAir()) {
                return false;
            } else {
                boolean flag = false;

                for(Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState blockstate1 = arg2.getBlockState(arg3.relative(direction));
                    if (blockstate1.is(this.plant)) {
                        if (flag) {
                            return false;
                        }

                        flag = true;
                    } else if (!blockstate1.isAir()) {
                        return false;
                    }
                }

                return flag;
            }
        } else {
            return true;
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{AGE});
    }

    public static void generatePlant(LevelAccessor arg, BlockPos arg2, RandomSource arg3, int i) {
        arg.setBlock(arg2, ((BrineTrunkPlantBlock)GenesisBlocks.BRINE_TRUNK.get()).getStateForPlacement(arg, arg2), 2);
        growTreeRecursive(arg, arg2, arg3, arg2, i, 0);
    }

    private static void growTreeRecursive(LevelAccessor arg, BlockPos arg2, RandomSource arg3, BlockPos arg4, int m, int n) {
        BrineTrunkPlantBlock brinetrunkplantblock = (BrineTrunkPlantBlock)GenesisBlocks.BRINE_TRUNK.get();
        int i = arg3.nextInt(4) + 1;
        if (n == 0) {
            ++i;
        }

        for(int j = 0; j < i; ++j) {
            BlockPos blockpos = arg2.above(j + 1);
            if (!allNeighborsEmpty(arg, blockpos, (Direction)null)) {
                return;
            }

            arg.setBlock(blockpos, brinetrunkplantblock.getStateForPlacement(arg, blockpos), 2);
            arg.setBlock(blockpos.below(), brinetrunkplantblock.getStateForPlacement(arg, blockpos.below()), 2);
        }

        boolean flag = false;
        if (n < 4) {
            int l = arg3.nextInt(4);
            if (n == 0) {
                ++l;
            }

            for(int k = 0; k < l; ++k) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(arg3);
                BlockPos blockpos1 = arg2.above(i).relative(direction);
                if (Math.abs(blockpos1.getX() - arg4.getX()) < m && Math.abs(blockpos1.getZ() - arg4.getZ()) < m && arg.isEmptyBlock(blockpos1) && arg.isEmptyBlock(blockpos1.below()) && allNeighborsEmpty(arg, blockpos1, direction.getOpposite())) {
                    flag = true;
                    arg.setBlock(blockpos1, brinetrunkplantblock.getStateForPlacement(arg, blockpos1), 2);
                    arg.setBlock(blockpos1.relative(direction.getOpposite()), brinetrunkplantblock.getStateForPlacement(arg, blockpos1.relative(direction.getOpposite())), 2);
                    growTreeRecursive(arg, blockpos1, arg3, arg4, m, n + 1);
                }
            }
        }

        if (!flag) {
            arg.setBlock(arg2.above(i), (BlockState)GenesisBlocks.BRINE_FLOWER.get().defaultBlockState().setValue(AGE, 5), 2);
        }

    }

    public void onProjectileHit(Level arg, BlockState arg2, BlockHitResult arg3, Projectile arg4) {
        BlockPos blockpos = arg3.getBlockPos();
        if (!arg.isClientSide && arg4.mayInteract(arg, blockpos) && arg4.getType().is(EntityTypeTags.IMPACT_PROJECTILES)) {
            arg.destroyBlock(blockpos, true, arg4);
        }

    }

    static {
        AGE = BlockStateProperties.AGE_5;
    }
}