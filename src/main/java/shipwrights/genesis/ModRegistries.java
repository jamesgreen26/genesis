package shipwrights.genesis;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import shipwrights.genesis.worldgen.AsteroidBelt;
import shipwrights.genesis.worldgen.AsteroidBlockSurfaceRule;
import shipwrights.genesis.worldgen.CraterNoise;
import shipwrights.genesis.worldgen.RandomNoise;

import static shipwrights.genesis.GenesisMod.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistries {
    @SubscribeEvent
    public static void onRegisterRegistries(RegisterEvent event) {
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
                    CraterNoise.resourceLocation,
                    CraterNoise.CODEC.codec()
            );
        });

        event.register(Registries.MATERIAL_RULE, helper -> {
            helper.register(ASTEROID_RULE_ID, AsteroidBlockSurfaceRule.CODEC.codec());
        });
    }
}
