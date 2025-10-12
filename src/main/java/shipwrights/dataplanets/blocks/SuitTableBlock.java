package shipwrights.dataplanets.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import shipwrights.dataplanets.MutableTags;
import shipwrights.dataplanets.items.SpaceArmourItem;
import shipwrights.dataplanets.registry.DPBlocks;

public class SuitTableBlock extends BaseEntityBlock {
    public SuitTableBlock(Properties arg) {
        super(arg);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos arg, BlockState arg2) {
        return new SuitTableBE(DPBlocks.SUIT_TABLE_BE.get(),arg,arg2);
    }

    @Override
    public RenderShape getRenderShape(BlockState arg) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState arg, Level arg2, BlockPos arg3, Player arg4, InteractionHand arg5, BlockHitResult arg6) {

        if(arg2 instanceof ServerLevel sl && arg5== InteractionHand.MAIN_HAND)
        {
            SuitTableBE be = (SuitTableBE) arg2.getBlockEntity(arg3);
            ItemStack hand = arg4.getMainHandItem();
            if(hand.is(MutableTags.SPACE_SUITS))
            {
                be.setSuitPiece(hand.copyAndClear());
                sl.playLocalSound(arg3.getX(),arg3.getY(),arg3.getZ(),SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS,1,1,true);
            }
            else
            {
                if(be.hasSuitPiece())
                {
                    if(hand.is(ItemTags.WOOL))
                    {
                        hand.shrink(1);
                        ItemStack upgraded = be.getSuitPiece();
                        SpaceArmourItem.addModule(upgraded,"has_heat");
                        arg4.addItem(upgraded.copy());
                        be.setSuitPiece(new CompoundTag());
                        sl.playLocalSound(arg3.getX(),arg3.getY(),arg3.getZ(),SoundEvents.ANVIL_USE, SoundSource.BLOCKS,1,1,true);
                    }
                    if(hand.is(Items.PACKED_ICE))
                    {
                        hand.shrink(1);
                        ItemStack upgraded = be.getSuitPiece();
                        SpaceArmourItem.addModule(upgraded,"has_cooling");
                        arg4.addItem(upgraded.copy());
                        be.setSuitPiece(new CompoundTag());
                        sl.playLocalSound(arg3.getX(),arg3.getY(),arg3.getZ(),SoundEvents.ANVIL_USE, SoundSource.BLOCKS,1,1,true);
                    }
                    if(hand.is(Items.WATER_BUCKET))
                    {
                        hand.shrink(1);
                        ItemStack upgraded = be.getSuitPiece();
                        SpaceArmourItem.addModule(upgraded,"has_oxygen");
                        arg4.addItem(upgraded.copy());
                        be.setSuitPiece(new CompoundTag());
                        sl.playLocalSound(arg3.getX(),arg3.getY(),arg3.getZ(),SoundEvents.ANVIL_USE, SoundSource.BLOCKS,1,1,true);
                    }
                }
            }
        }

        return super.use(arg, arg2, arg3, arg4, arg5, arg6);
    }
}
