package shipwrights.genesis.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.GenesisMod;

@Mixin(value = ClientLevel.class, priority = 1500)
public class VSClientLevelBiomeMixin {

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.client.world.MixinClientLevel",
            name = "animateTickVS"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean genesis$skipBiomeParticlesInSpace(BlockState blockState, BlockGetter level, BlockPos pos, Operation<Boolean> original) {
        if (level instanceof ClientLevel clientLevel && GenesisMod.isSpaceDimension(clientLevel)) {
            return true;
        }
        return original.call(blockState, level, pos);
    }
}