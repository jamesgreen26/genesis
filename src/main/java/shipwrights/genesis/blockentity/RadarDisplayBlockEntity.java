package shipwrights.genesis.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RadarDisplayBlockEntity extends BlockEntity {

    public RadarDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.RADAR_DISPLAY.get(), pos, state);
    }
}
