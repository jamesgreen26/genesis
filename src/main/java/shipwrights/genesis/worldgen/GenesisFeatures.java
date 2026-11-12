package shipwrights.genesis.worldgen;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;

/// Feature types used by Genesis's configured features
public final class GenesisFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, GenesisMod.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CREVASSE =
            FEATURES.register("crevasse", () -> new CrevasseFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ICE_PILLAR =
            FEATURES.register("ice_pillar", () -> new IcePillarFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ICE_CRYSTAL =
            FEATURES.register("ice_crystal", () -> new IceCrystalFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GEOTHERMAL =
            FEATURES.register("geothermal", () -> new GeothermalFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BASALT_FORMATION =
            FEATURES.register("basalt_formation", () -> new BasaltFormationFeature(NoneFeatureConfiguration.CODEC));

    private GenesisFeatures() {
    }
}
