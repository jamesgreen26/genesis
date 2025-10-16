package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.blocks.*;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DPBlocks {

    public static BlockEntry<ResearchStationBlock> RESEARCH_STATION = Reg.REGISTRATE.block("research_computer", ResearchStationBlock::new).simpleItem().register();
    public static BlockEntityEntry<ResearchStationBE> RESEARCH_STATION_BE = Reg.REGISTRATE.blockEntity("research_station",ResearchStationBE::new).validBlock(RESEARCH_STATION).register();

    public static BlockEntry<SuitTableBlock> SUIT_TABLE = Reg.REGISTRATE.block("suit_table", SuitTableBlock::new).simpleItem().register();
    public static BlockEntityEntry<SuitTableBE> SUIT_TABLE_BE = Reg.REGISTRATE.blockEntity("suit_table_be",SuitTableBE::new).validBlock(SUIT_TABLE).register();


    public static BlockEntry<GasBlock> DENSE_GAS = Reg.REGISTRATE.block("dense_gas", a->new GasBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().noOcclusion())).register();

    public static void init()
    {

    }
}
