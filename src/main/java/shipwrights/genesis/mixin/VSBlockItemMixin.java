package shipwrights.genesis.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;

@Mixin(value = BlockItem.class, priority = 1500)
public class VSBlockItemMixin {

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.feature.block_placement.MixinBlockItem",
            name = "checkObstructionByPlayer"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true)
    private void genesis$scaleExplosionForce(Level level, BlockState blockState, BlockPos blockPos, CollisionContext ctx, Operation<Boolean> original, BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<Boolean> cir) {
        if (GenesisMod.isMiniScale(level)) {
            cir.setReturnValue(original.call(level, blockState, blockPos, ctx));
        }
    }
}
