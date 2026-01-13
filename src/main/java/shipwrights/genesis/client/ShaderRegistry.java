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
import team.lodestar.lodestone.systems.rendering.StateShards;
import team.lodestar.lodestone.systems.rendering.shader.ShaderHolder;

import java.util.concurrent.ConcurrentHashMap;

import static team.lodestar.lodestone.registry.client.LodestoneShaderRegistry.registerShader;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShaderRegistry {

    public static final ShaderHolder SUN_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun"), DefaultVertexFormat.POSITION_TEX);
    public static final ShaderHolder RAYMARCH_PROCEDURAL_PLANET_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "raymarch_procedural_planet"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
    public static final ShaderHolder WORMHOLE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole"), DefaultVertexFormat.POSITION_COLOR_TEX);
    public static final ShaderHolder RAYMARCH_TEXTURED_PLANET_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "raymarch_textured_planet"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);


    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        registerShader(event, SUN_SHADER);
        registerShader(event, RAYMARCH_PROCEDURAL_PLANET_SHADER);
        registerShader(event, WORMHOLE_SHADER);
        registerShader(event, RAYMARCH_TEXTURED_PLANET_SHADER);
    }

    private static LodestoneRenderType SUN_RENDER_TYPE;
    private static LodestoneRenderType PLANET_RENDER_TYPE;
    private static final ConcurrentHashMap<ResourceLocation, LodestoneRenderType> RAYMARCH_TEXTURED_PLANET_RENDER_TYPES = new ConcurrentHashMap<>();

    public static LodestoneRenderType getSunRenderType() {
        if (SUN_RENDER_TYPE == null) {
            SUN_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("sun_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(SUN_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return SUN_RENDER_TYPE;
    }

    public static LodestoneRenderType getRaymarchProceduralPlanetRenderType() {
        if (PLANET_RENDER_TYPE == null) {
            PLANET_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("raymarch_procedural_planet_render_type", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(RAYMARCH_PROCEDURAL_PLANET_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return PLANET_RENDER_TYPE;
    }

    public static LodestoneRenderType getRaymarchTexturedPlanetRenderType(ResourceLocation textureLocation) {
        return RAYMARCH_TEXTURED_PLANET_RENDER_TYPES.computeIfAbsent(textureLocation, loc -> {
            // Build the full texture path
            ResourceLocation fullTexturePath = ResourceLocation.fromNamespaceAndPath(
                loc.getNamespace(),
                "textures/" + loc.getPath() + ".png"
            );

            return LodestoneRenderTypeRegistry.createGenericRenderType(
                "raymarch_textured_planet_" + loc.getNamespace() + "_" + loc.getPath().replace("/", "_") + "_render_type",
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                LodestoneRenderTypeRegistry.builder()
                    .setShaderState(RAYMARCH_TEXTURED_PLANET_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
                    .setTextureState(new RenderStateShard.TextureStateShard(fullTexturePath, false, false))
            );
        });
    }

    /**
     * Clears cached textured planet render types. Call when resources are reloaded.
     */
    public static void clearTexturedPlanetRenderTypes() {
        RAYMARCH_TEXTURED_PLANET_RENDER_TYPES.clear();
    }
}