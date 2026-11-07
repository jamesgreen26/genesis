package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

/// Density function types used by Genesis's noise settings, such as the moon's craters.
public final class GenesisDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, GenesisMod.MOD_ID);

    public static final RegistryObject<Codec<? extends DensityFunction>> MULTI_CRATER =
            DENSITY_FUNCTION_TYPES.register(MultiCraterNoise.resourceLocation.getPath(), () -> MultiCraterNoise.CODEC.codec());

    private GenesisDensityFunctions() {
    }
}
