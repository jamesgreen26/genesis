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
    public static final ShaderHolder PLANET_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet"), DefaultVertexFormat.POSITION_COLOR_TEX);
    public static final ShaderHolder PLANET_TEXTURED_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_textured"), DefaultVertexFormat.POSITION_COLOR_TEX);
    public static final ShaderHolder PLANET_MASK_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_mask"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder PLANET_SHADOW_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet_shadow"), DefaultVertexFormat.POSITION_COLOR);
    public static final ShaderHolder WORMHOLE_SHADER = new ShaderHolder(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole"), DefaultVertexFormat.POSITION_COLOR_TEX);

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        registerShader(event, SUN_SHADER);
        registerShader(event, PLANET_SHADER);
        registerShader(event, PLANET_TEXTURED_SHADER);
        registerShader(event, PLANET_MASK_SHADER);
        registerShader(event, PLANET_SHADOW_SHADER);
        registerShader(event, WORMHOLE_SHADER);
    }

    private static LodestoneRenderType SUN_RENDER_TYPE;
    private static LodestoneRenderType PLANET_RENDER_TYPE;
    private static LodestoneRenderType PLANET_MASK_RENDER_TYPE;
    private static final ConcurrentHashMap<ResourceLocation, LodestoneRenderType> TEXTURED_PLANET_RENDER_TYPES = new ConcurrentHashMap<>();

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

    public static LodestoneRenderType getPlanetMaskRenderType() {
        if (PLANET_MASK_RENDER_TYPE == null) {
            PLANET_MASK_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType("planet_mask_render_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, LodestoneRenderTypeRegistry.builder()
                    .setShaderState(PLANET_MASK_SHADER)
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("no_transparency", RenderSystem::disableBlend, () -> {}))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return PLANET_MASK_RENDER_TYPE;
    }

    private static LodestoneRenderType PLANET_SHADOW_RENDER_TYPE;

    public static LodestoneRenderType getPlanetShadowRenderType() {
        if (PLANET_SHADOW_RENDER_TYPE == null) {
            PLANET_SHADOW_RENDER_TYPE = LodestoneRenderTypeRegistry.createGenericRenderType(
                "planet_shadow_render_type",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                LodestoneRenderTypeRegistry.builder()
                    .setShaderState(PLANET_SHADOW_SHADER)
                    .setTransparencyState(StateShards.NORMAL_TRANSPARENCY)
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                    .setCullState(LodestoneRenderTypeRegistry.CULL)
            );
        }
        return PLANET_SHADOW_RENDER_TYPE;
    }

    /**
     * Gets a render type for a textured planet.
     *
     * @param textureLocation The texture location (without textures/ prefix or .png extension)
     * @return A render type that uses the textured planet shader with the specified texture
     */
    public static LodestoneRenderType getTexturedPlanetRenderType(ResourceLocation textureLocation) {
        return TEXTURED_PLANET_RENDER_TYPES.computeIfAbsent(textureLocation, loc -> {
            // Build the full texture path
            ResourceLocation fullTexturePath = ResourceLocation.fromNamespaceAndPath(
                loc.getNamespace(),
                "textures/" + loc.getPath() + ".png"
            );

            return LodestoneRenderTypeRegistry.createGenericRenderType(
                "planet_textured_" + loc.getNamespace() + "_" + loc.getPath().replace("/", "_"),
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                LodestoneRenderTypeRegistry.builder()
                    .setShaderState(PLANET_TEXTURED_SHADER)
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
        TEXTURED_PLANET_RENDER_TYPES.clear();
    }
}
