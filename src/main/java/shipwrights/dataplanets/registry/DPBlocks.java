package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.blocks.*;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DPBlocks {

    public static BlockEntry<SuitTableBlock> SUIT_TABLE = DataplanetsMod.REGISTRATE.block("suit_table", SuitTableBlock::new).simpleItem().register();
    public static BlockEntityEntry<SuitTableBE> SUIT_TABLE_BE = DataplanetsMod.REGISTRATE.blockEntity("suit_table_be",SuitTableBE::new).validBlock(SUIT_TABLE).register();

    public static BlockEntry<OxygenBubbleBlock> OXYGEN_BUBBLE = DataplanetsMod.REGISTRATE.block("oxygen_bubble", OxygenBubbleBlock::new).simpleItem().register();
    public static BlockEntityEntry<OxygenBubbleBE> OXYGEN_BUBBLE_BE = DataplanetsMod.REGISTRATE.blockEntity("oxygen_bubble_be",OxygenBubbleBE::new).validBlock(OXYGEN_BUBBLE).register();


    public static BlockEntry<GasBlock> DENSE_GAS = DataplanetsMod.REGISTRATE.block("dense_gas", a->new GasBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().noOcclusion())).register();

    /// do not delete
    public static void init() {}
}
