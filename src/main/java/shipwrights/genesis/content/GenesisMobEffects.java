package shipwrights.genesis.content;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

public final class GenesisMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, GenesisMod.MOD_ID);

    public static final DeferredHolder<MobEffect, WebbedEffect> WEBBED = MOB_EFFECTS.register("webbed", WebbedEffect::new);

    private GenesisMobEffects() {
    }
}
