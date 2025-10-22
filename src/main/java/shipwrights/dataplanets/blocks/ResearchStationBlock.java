package shipwrights.dataplanets.blocks;

import shipwrights.dataplanets.ClientThings;
import shipwrights.dataplanets.registry.DPBlocks;
import shipwrights.dataplanets.registry.DPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ResearchStationBlock extends BaseEntityBlock {
    public ResearchStationBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos p_60505_, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        ResearchStationBE researchStationBE = (ResearchStationBE) level.getBlockEntity(p_60505_);
        if(level.isClientSide)
        {
            ClientThings.openResearchScreen(researchStationBE);
        }
        else
        {

            if(hand==InteractionHand.MAIN_HAND)
            {
                ItemStack stack = player.getItemInHand(hand);

                if(stack.is(DPItems.RESEARCH.get()))
                {
                    if(stack.hasTag())
                    {
                        CompoundTag tag = player.getItemInHand(hand).getTag();
                        boolean v = researchStationBE.addDatapoint(
                                tag.getString("biome"),
                                tag.getInt("sectorX"),
                                tag.getInt("sectorZ"),
                                tag.getLong("time"));
                        if(v)
                        {
                            player.sendSystemMessage(Component.empty().append("Added research to this station!"));
                            stack.shrink(1);
                        }
                    }
                    else
                    {
                        researchStationBE.addGenericDatapoint();
                        player.sendSystemMessage(Component.empty().append("Added research to this station!"));
                        stack.shrink(1);
                    }

                }
            }


        }
        return super.use(p_60503_,level, p_60505_, player, hand, p_60508_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49915_) {
        super.createBlockStateDefinition(p_49915_.add(BlockStateProperties.HORIZONTAL_FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_49820_) {
        return super.getStateForPlacement(p_49820_).setValue(BlockStateProperties.HORIZONTAL_FACING,p_49820_.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return Block.box(2,0,2,14,2,14);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return new ResearchStationBE(DPBlocks.RESEARCH_STATION_BE.get(),p_153215_,p_153216_);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return createTickerHelper(p_153214_,DPBlocks.RESEARCH_STATION_BE.get(),ResearchStationBE::tick);
    }
}
