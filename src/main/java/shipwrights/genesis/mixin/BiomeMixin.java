package shipwrights.genesis.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeMixin {
    /// Vanilla only checks the temperature when deciding whether snow can settle, so cold biomes without precipitation
    /// (like the moon's, which share the overworld's weather) still pile up snow whenever it's raining. Never let snow
    /// settle in a biome without precipitation. No vanilla biome is affected, as all those without are warm
    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void genesis$noSnowWithoutPrecipitation(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!((Biome) (Object) this).hasPrecipitation()) {
            cir.setReturnValue(false);
        }
    }
}
