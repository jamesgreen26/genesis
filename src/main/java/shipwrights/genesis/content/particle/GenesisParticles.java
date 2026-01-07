package shipwrights.genesis.content.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public class GenesisParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GenesisMod.MOD_ID);


    public static final RegistryObject<SimpleParticleType> ZAP_BUBBLE_PARTICLES =
            PARTICLE_TYPES.register("zap_bubble_particles", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> VERDITE_PARTICLES =
            PARTICLE_TYPES.register("verdite_particles", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus){
        PARTICLE_TYPES.register(eventBus);
    }
}
