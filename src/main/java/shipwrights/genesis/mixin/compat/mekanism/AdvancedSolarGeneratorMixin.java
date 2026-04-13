package shipwrights.genesis.mixin.compat.mekanism;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;

@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityAdvancedSolarGenerator", remap = false)
public abstract class AdvancedSolarGeneratorMixin {

    @Shadow
    protected abstract mekanism.api.math.FloatingLong getConfiguredMax();

    @Inject(method = "checkCanSeeSun", at = @At("HEAD"), cancellable = true)
    private void genesis$checkCanSeeSun(CallbackInfoReturnable<Boolean> cir) {
        Level level = ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getLevel();
        if (level != null && GenesisMod.isSpaceDimension(level)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getProduction", at = @At("HEAD"), cancellable = true)
    private void genesis$getProduction(CallbackInfoReturnable<Object> cir) {
        Level level = ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getLevel();
        if (level != null && GenesisMod.isSpaceDimension(level)) {
            cir.setReturnValue(getConfiguredMax());
        }
    }
}
