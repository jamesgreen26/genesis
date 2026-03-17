package shipwrights.genesis.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Rather sussy class to store global client variables, mostly used in mixins
 */
@OnlyIn(Dist.CLIENT)
public class ClientStorage {
    /**
     * True for a couple of ticks while the player is entering or leaving the wormhole dimension.
     * Used for {@link shipwrights.genesis.mixin.MinecraftMixin}.
     * <br>
     * Set to true by the Void engine in {@link shipwrights.genesis.content.blockentity.VoidEngineInterfaceBlockEntity#tick(Level, BlockPos, BlockState, BlockEntity)}
     * <br>
     * Set to false by {@link WarpLoadingMenu#onClose()}
     */
    public static boolean goingToFromWormhole = false;
}
