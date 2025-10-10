package shipwrights.genesis.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.GenesisMod;

@Mixin(HugeExplosionSeedParticle.class)
abstract public class HugeExplosionSeedParticleMixin extends Particle {

    protected HugeExplosionSeedParticleMixin(ClientLevel arg, double d, double e, double f) {
        super(arg, d, e, f);
    }

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 4.0))
    private double modifyExplosionSpread(double original) {
        if (GenesisMod.isMiniScale(this.level)) {
            return 0.25;
        } else {
            return original;
        }
    }
}