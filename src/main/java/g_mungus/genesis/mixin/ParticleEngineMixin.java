package g_mungus.genesis.mixin;

import g_mungus.genesis.GenesisMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    public void createParticleMixin(ParticleOptions arg, double d, double e, double f, double g, double h, double i, CallbackInfoReturnable<Particle> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().location().equals(GenesisMod.SPACE_DIM)) {

            if (arg.getType().equals(ParticleTypes.BLOCK)) {
                cir.setReturnValue(null);
            }

            Particle particle = this.makeParticle(arg, d, e, f, g, h, i);
            if (particle != null) {

                if (particle instanceof BreakingItemParticle) {
                    return;
                }

                particle.scale(1/16f);
                this.add(particle);
                cir.setReturnValue(particle);
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    public void destroyMixin(BlockPos arg, BlockState arg2, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    public void crackMixin(BlockPos arg, Direction arg2, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
            ci.cancel();
        }
    }


    @Shadow
    public void add(Particle particle) {
    }

    @Shadow
    private Particle makeParticle(ParticleOptions arg, double d, double e, double f, double g, double h, double i) {
        return null;
    }
}
