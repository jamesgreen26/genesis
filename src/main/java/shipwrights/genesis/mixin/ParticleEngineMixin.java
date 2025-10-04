package shipwrights.genesis.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    public void createParticleMixin(ParticleOptions arg, double d, double e, double f, double g, double h, double i, CallbackInfoReturnable<Particle> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && GenesisMod.isMiniScale(level)) {

            Particle particle = this.makeParticle(arg, d, e, f, g, h, i);
            if (particle != null) {

                particle.scale(1 / 16f);
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
        if (level != null && GenesisMod.isMiniScale(level)) {

            for (int i = 0; i < 10; i++) {
                level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, arg2),
                        arg.getX() + Math.random(),
                        arg.getY() + Math.random(),
                        arg.getZ() + Math.random(),
                        0,
                        0,
                        0
                );
            }

            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    public void crackMixin(BlockPos arg, Direction arg2, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || GenesisMod.isMiniScale(level)) {
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
