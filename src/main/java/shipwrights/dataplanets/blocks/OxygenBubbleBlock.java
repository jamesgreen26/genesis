package shipwrights.dataplanets.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import shipwrights.dataplanets.registry.DPBlocks;

public class OxygenBubbleBlock extends BaseEntityBlock {
    public OxygenBubbleBlock(Properties arg) {
        super(arg);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos arg, BlockState arg2) {
        return new OxygenBubbleBE(DPBlocks.OXYGEN_BUBBLE_BE.get(), arg,arg2);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level arg, BlockState arg2, BlockEntityType<T> arg3) {
        return createTickerHelper(arg3, DPBlocks.OXYGEN_BUBBLE_BE.get(),OxygenBubbleBE::tick);
    }
}
