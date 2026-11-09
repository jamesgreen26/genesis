package shipwrights.genesis.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public final class GenesisMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GenesisMod.MOD_ID);

    public static final RegistryObject<MobEffect> WEBBED = MOB_EFFECTS.register("webbed", WebbedEffect::new);

    private GenesisMobEffects() {
    }
}
