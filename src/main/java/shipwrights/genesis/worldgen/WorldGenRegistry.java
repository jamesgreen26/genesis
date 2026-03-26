package shipwrights.genesis.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import shipwrights.genesis.GenesisMod;

import static shipwrights.genesis.GenesisMod.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class WorldGenRegistry {
    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, helper -> {
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "random_noise"),
                    RandomNoise.MAP_CODEC.codec()
            );
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "asteroid_belt"),
                    AsteroidBelt.MAP_CODEC.codec()
            );
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "radial_gradient"),
                    RadialGradientDensity.MAP_CODEC.codec()
            );
            helper.register(
                    CraterNoise.resourceLocation,
                    CraterNoise.CODEC.codec()
            );

            helper.register(
                    MultiCraterNoise.resourceLocation,
                    MultiCraterNoise.CODEC.codec()
            );
        });

        event.register(Registries.MATERIAL_RULE, helper -> {
            helper.register(ASTEROID_RULE_ID, AsteroidBlockSurfaceRule.CODEC.codec());
        });
    }
}
