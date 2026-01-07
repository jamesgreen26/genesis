package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NavProjectorBlockEntity extends BlockEntity {

    public NavProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.NAV_PROJECTOR.get(), pos, state);
    }
}
