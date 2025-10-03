package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.registry.client.LodestoneRenderTypeRegistry;
import team.lodestar.lodestone.systems.rendering.LodestoneRenderType;
import team.lodestar.lodestone.systems.rendering.shader.ShaderHolder;

import static team.lodestar.lodestone.registry.client.LodestoneShaderRegistry.registerShader;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShaderRegistry {

    public static final ShaderHolder SUN_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun"), DefaultVertexFormat.POSITION_TEX);
    public static final ShaderHolder PLANET_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet"), DefaultVertexFormat.POSITION_COLOR_TEX);

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        registerShader(event, SUN_SHADER);
        registerShader(event, PLANET_SHADER);
    }

    private static LodestoneRenderType SUN_RENDER_TYPE;
    private static LodestoneRenderType PLANET_RENDER_TYPE;

    public static LodestoneRenderType getSunRenderType() {
        if (SUN_RENDER_TYPE == null) {
            SUN_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("sun_render_type", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return SUN_RENDER_TYPE;
    }

    public static LodestoneRenderType getPlanetRenderType() {
        if (PLANET_RENDER_TYPE == null) {
            PLANET_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("planet_render_type", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(PLANET_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return PLANET_RENDER_TYPE;
    }
}
