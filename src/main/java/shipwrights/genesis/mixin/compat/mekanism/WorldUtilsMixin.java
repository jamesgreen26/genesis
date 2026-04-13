package shipwrights.genesis.mixin.compat.mekanism;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import shipwrights.genesis.GenesisMod;

@Pseudo
@Mixin(targets = "mekanism.common.util.WorldUtils", remap = false)
public class WorldUtilsMixin {

    @WrapMethod(method = "getSunBrightness")
    private static float getSunBrightnessWrap(Level world, float partialTicks, Operation<Float> original) {
        if (GenesisMod.isSpaceDimension(world)) return 1f;

        return original.call(world, partialTicks);
    }

    @WrapMethod(method = "canSeeSun")
    private static boolean canSeeSunWrap(Level world, BlockPos pos, Operation<Boolean> original) {
        if (GenesisMod.isSpaceDimension(world)) return true;

        return original.call(world, pos);
    }
}
