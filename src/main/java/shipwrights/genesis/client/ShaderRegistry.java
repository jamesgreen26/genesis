package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.systems.rendering.shader.ShaderHolder;

import static team.lodestar.lodestone.registry.client.LodestoneShaderRegistry.registerShader;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShaderRegistry {

    public static final ShaderHolder SUN_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun"), DefaultVertexFormat.POSITION_TEX);

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        registerShader(event, SUN_SHADER);
    }
}
