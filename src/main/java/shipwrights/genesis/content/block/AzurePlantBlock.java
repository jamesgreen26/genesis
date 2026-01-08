package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraftforge.common.IPlantable;
import shipwrights.genesis.content.GenesisTags;

public class AzurePlantBlock extends Block implements IPlantable {
    public AzurePlantBlock(BlockBehaviour.Properties arg) {
        super(arg);
    }

    protected boolean mayPlaceOn(BlockState arg, BlockGetter arg2, BlockPos arg3) {
        return arg.is(GenesisTags.Blocks.BLUE_MOSSES);
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        return !arg.canSurvive(arg4, arg5) ? Blocks.AIR.defaultBlockState() : super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        BlockPos blockpos = arg3.below();
        return arg2.getBlockState(blockpos).is(GenesisTags.Blocks.BLUE_MOSSES);
//        return arg.getBlock() == this ? arg2.getBlockState(blockpos).canSustainPlant(arg2, blockpos, Direction.UP, this) : this.mayPlaceOn(arg2.getBlockState(blockpos), arg2, blockpos);
    }

    public boolean propagatesSkylightDown(BlockState arg, BlockGetter arg2, BlockPos arg3) {
        return arg.getFluidState().isEmpty();
    }

    public boolean isPathfindable(BlockState arg, BlockGetter arg2, BlockPos arg3, PathComputationType arg4) {
        return arg4 == PathComputationType.AIR && !this.hasCollision ? true : super.isPathfindable(arg, arg2, arg3, arg4);
    }

    public BlockState getPlant(BlockGetter world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() != this ? this.defaultBlockState() : state;
    }
}