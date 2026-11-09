package shipwrights.genesis.worldgen;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

/// Feature types used by Genesis's configured features
public final class GenesisFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, GenesisMod.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CREVASSE =
            FEATURES.register("crevasse", () -> new CrevasseFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ICE_PILLAR =
            FEATURES.register("ice_pillar", () -> new IcePillarFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ICE_CRYSTAL =
            FEATURES.register("ice_crystal", () -> new IceCrystalFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GEOTHERMAL =
            FEATURES.register("geothermal", () -> new GeothermalFeature(NoneFeatureConfiguration.CODEC));

    private GenesisFeatures() {
    }
}
