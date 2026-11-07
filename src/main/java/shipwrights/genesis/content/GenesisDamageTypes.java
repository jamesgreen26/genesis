package shipwrights.genesis.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import shipwrights.genesis.GenesisMod;

/// Damage types defined by Genesis's data, under {@code data/genesis/damage_type}
public final class GenesisDamageTypes {
    public static final ResourceKey<DamageType> FALLING_ICICLE =
            ResourceKey.create(Registries.DAMAGE_TYPE, GenesisMod.id("falling_icicle"));

    private GenesisDamageTypes() {
    }
}
