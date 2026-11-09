package shipwrights.genesis.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("gravity")
    float genesis$getGravity();

    @Accessor("gravity")
    void genesis$setGravity(float gravity);
}
