package shipwrights.dataplanets.systemCreation.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.systemCreation.dimension.biome.features.CrystalFeature;
import shipwrights.dataplanets.systemCreation.dimension.noise.AlienDensityFunction;
import shipwrights.dataplanets.systemCreation.dimension.noise.Crater;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registration {
    @SubscribeEvent
    public static void onRegisterRegistries(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, helper -> {
            helper.register(
                    Crater.resourceLocation,
                    Crater.CODEC.codec()
            );

            helper.register(
                    AlienDensityFunction.getResourceLocation(),
                    AlienDensityFunction.CODEC.codec()
            );
        });

        event.register(Registries.FEATURE, helper -> {
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, "crystal"),
                    new CrystalFeature(NoneFeatureConfiguration.CODEC)
            );
        });
    }
}
