package shipwrights.genesis.content.block;

import shipwrights.genesis.content.blockentity.VoidCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class VoidEngineViewportBlock extends GlassBlock {
    public VoidEngineViewportBlock() {
        super(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GLASS)
                .strength(3.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion());
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        VoidCoreBlockEntity.updateVoidCore(pos, level);
    }

    @Override
    public void onRemove(BlockState arg, Level arg2, BlockPos arg3, BlockState arg4, boolean bl) {
        super.onRemove(arg, arg2, arg3, arg4, bl);
        VoidCoreBlockEntity.updateVoidCore(arg3, arg2);
    }
}
