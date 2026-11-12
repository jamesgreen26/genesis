package shipwrights.genesis.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

/// Density function types used by Genesis's noise settings, such as the moon's craters.
public final class GenesisDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, GenesisMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<? extends DensityFunction>> MULTI_CRATER =
            DENSITY_FUNCTION_TYPES.register(MultiCraterNoise.resourceLocation.getPath(), () -> MultiCraterNoise.CODEC.codec());

    private GenesisDensityFunctions() {
    }
}
