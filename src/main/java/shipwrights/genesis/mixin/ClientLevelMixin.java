package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @WrapOperation(
            method = "lambda$doAnimateTick$8",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/AmbientParticleSettings;canSpawn(Lnet/minecraft/util/RandomSource;)Z"
            ),
            remap = false
    )
    private boolean genesis$fadeBiomeParticles(
            AmbientParticleSettings settings,
            RandomSource random,
            Operation<Boolean> original
    ) {
        boolean base = original.call(settings, random);
        if (!base) {
            return false;
        }

        double camY = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
        double densityFade = 1.0 - Mth.clamp((camY - 300.0) / 60.0, 0.0, 1.0);

        return densityFade >= 1.0 || random.nextFloat() < densityFade;
    }
}
