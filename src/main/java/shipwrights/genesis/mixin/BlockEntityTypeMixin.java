package shipwrights.genesis.mixin;

import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin {
    /// Vanilla's brushable block entity type only lists suspicious sand and gravel as valid, which stops our
    /// brushable blocks' block entities rendering the item being uncovered. Accept any brushable block instead
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void genesis$acceptAllBrushableBlocks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityType.BRUSHABLE_BLOCK && state.getBlock() instanceof BrushableBlock) {
            cir.setReturnValue(true);
        }
    }
}
