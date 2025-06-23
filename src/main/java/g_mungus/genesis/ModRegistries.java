package g_mungus.genesis;

import g_mungus.genesis.worldgen.AsteroidBelt;
import g_mungus.genesis.worldgen.AsteroidBlockSurfaceRule;
import g_mungus.genesis.worldgen.RandomNoise;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModRegistries {

    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath("genesis", "asteroid_block_surface_rule");

    @SubscribeEvent
    public static void onRegisterRegistries(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, helper -> {
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "random_noise"),
                    RandomNoise.MAP_CODEC
            );
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "asteroid_belt"),
                    AsteroidBelt.MAP_CODEC
            );
        });

        event.register(Registries.MATERIAL_RULE, helper -> {
            helper.register(ASTEROID_RULE_ID, AsteroidBlockSurfaceRule.CODEC.codec());
        });
    }
}
