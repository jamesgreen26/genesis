package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Unique
    public final RandomSource genesis$random = RandomSource.create();

    @ModifyExpressionValue(
            method = "doAnimateTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;getAmbientParticle()Ljava/util/Optional;"
            )
    )
    private Optional<AmbientParticleSettings> genesis$fadeBiomeParticles(
            Optional<AmbientParticleSettings> original
    ) {
        if (original.isEmpty()) {
            return original;
        }
        double camY = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
        double densityFade = 1.0 - Mth.clamp((camY - 300.0) / 60.0, 0.0, 1.0);

        if (densityFade >= 1.0) {
            return original;
        }

        return this.genesis$random.nextFloat() < densityFade ? original : Optional.empty();
    }
}
