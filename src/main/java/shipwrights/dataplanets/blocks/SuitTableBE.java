package shipwrights.dataplanets.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import team.lodestar.lodestone.helpers.NBTHelper;

public class SuitTableBE extends BlockEntity {

    public CompoundTag suitPiece = new CompoundTag();

    public SuitTableBE(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    @Override
    protected void saveAdditional(CompoundTag arg) {
        arg.put("suitPiece",suitPiece);
        super.saveAdditional(arg);
    }

    @Override
    public void load(CompoundTag arg) {
        super.load(arg);
        suitPiece = arg.getCompound("suitPiece");
    }

    public void setSuitPiece(CompoundTag suitPiece) {
        this.suitPiece = suitPiece;
        setChanged();
    }

    public boolean hasSuitPiece()
    {
        return !this.suitPiece.isEmpty();
    }

    public ItemStack getSuitPiece() {
        ItemStack stack = ItemStack.of(suitPiece);

        return stack;
    }

    public void setSuitPiece(ItemStack stack)
    {
        CompoundTag piece = stack.serializeNBT();
        setSuitPiece(piece);
    }

}
