package shipwrights.genesis.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.RisingParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.GenesisMod;

@Mixin(RisingParticle.class)
public abstract class RisingParticleMixin {

    @ModifyConstant(
        method = "<init>",
        constant = @Constant(floatValue = 0.05F)
    )
    private float genesis$reduceRandomOffset(float original, ClientLevel p_107631_, double p_107632_, double p_107633_, double p_107634_, double p_107635_, double p_107636_, double p_107637_) {
        return (float) (original * GenesisMod.getDimensionScale(p_107631_));
    }
}