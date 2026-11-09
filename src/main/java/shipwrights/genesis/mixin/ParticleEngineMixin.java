package shipwrights.genesis.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.space.Gravity;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    protected ClientLevel level;

    /// Scales particles' gravity by {@link Gravity} as they're added, once their constructors have set it. Covers any
    /// particle moved by its {@code gravity} field, including buoyant ones with negative gravity, as buoyancy scales with
    /// gravity too
    @Inject(method = "add", at = @At("HEAD"))
    private void genesis$scaleGravity(Particle particle, CallbackInfo ci) {
        if (level == null) return;
        double multiplier = Gravity.multiplier(level);
        if (multiplier != 1) {
            ParticleAccessor accessor = (ParticleAccessor) particle;
            accessor.genesis$setGravity((float) (accessor.genesis$getGravity() * multiplier));
        }
    }
}
