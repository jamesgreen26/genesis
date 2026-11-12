package shipwrights.genesis.mixin;

import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// The game test server makes its world from the flat preset alone, leaving out the dimensions data packs add, as
/// Genesis's are. Give it those too, as a dedicated server has, so tests can travel between them. Forge's game test
/// server used to load the world as a dedicated server does, so had them already
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    @ModifyArg(method = "lambda$create$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static Registry<LevelStem> genesis$withDataPackDimensions(Registry<LevelStem> dimensions,
                                                                      @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
    }
}
