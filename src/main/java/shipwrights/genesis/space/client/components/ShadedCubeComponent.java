package shipwrights.genesis.space.client.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Daylight;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.client.BlendedShaderInstance;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.CubeFace;
import shipwrights.genesis.space.client.TextureSlot;
import shipwrights.genesis.space.client.shading.CelestialShadows;
import shipwrights.genesis.space.client.shading.ShadowVolume;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.util.MoreCodecs;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/// A textured cube lit by the nearest light-casting celestial, optionally with shadows cast by other celestials.
///
/// The texture is {@code genesis:celestials/<namespace>/<path>}, named after the celestial's id.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record ShadedCubeComponent(
        boolean drawShadows
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("shaded_cube");

    private static ShaderInstance texturedShader;

    // Bound in place of the emissive texture when a planet doesn't have one, so it emits nothing
    private static final ResourceLocation NO_EMISSIVE_TEXTURE = GenesisMod.id("textures/celestials/no_emissive.png");

    private static final ConcurrentHashMap<ResourceLocation, RenderType> TEXTURED_RENDER_TYPES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("planet_textured"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), shader -> texturedShader = shader);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        // Textured render types check whether an emissive texture exists, so must be rebuilt when resource packs change
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> TEXTURED_RENDER_TYPES.clear());
        // Likewise when an editor puts textures in place to preview them
        CelestialVisualizer.onTexturesChanged(TEXTURED_RENDER_TYPES::clear);
    }

    @Override
    public List<TextureSlot> textures(ResourceLocation celestialId) {
        ResourceLocation texture = textureLocation(celestialId);
        return List.of(
                new TextureSlot(texture.withPath(path -> "textures/" + path + ".png"),
                        Component.translatable("genesis.texture.shaded_cube"), true,
                        Component.translatable("genesis.texture.shaded_cube.hint"), true),
                new TextureSlot(texture.withPath(path -> "textures/" + path + "_emissive.png"),
                        Component.translatable("genesis.texture.shaded_cube_emissive"), false,
                        Component.translatable("genesis.texture.shaded_cube_emissive.hint"))
        );
    }

    /// The cube's texture for the celestial {@code id}, without the {@code textures/} prefix or {@code .png} extension
    private static ResourceLocation textureLocation(ResourceLocation id) {
        return GenesisMod.id("celestials/" + id.getNamespace() + "/" + id.getPath());
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void render(CelestialRenderContext context) {
        try {
            renderCube(context);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
        }
    }

    private void renderCube(CelestialRenderContext context) {
        Celestial toRender = context.toRender();
        VantagePoint vantagePoint = context.vantagePoint();
        Time time = context.time();

        Registry<Celestial> registry = context.celestials();
        Vector3dc position = toRender.transformProvider().getPosition(registry, time);
        Quaterniondc rotation = toRender.transformProvider().getRotation(registry, time);

        // Nothing to light the cube with
        Optional<Vector3dc> starPosition = Daylight.nearestLightSource(registry, toRender, position, time);
        if (starPosition.isEmpty()) return;

        List<ShadowVolume> shadows = drawShadows
                ? computeShadows(registry, toRender, position, rotation, starPosition.get(), time)
                : List.of();

        Vector3d lightDir = new Vector3d(position).sub(starPosition.get())
                .rotate(vantagePoint.getRotation().conjugate(new Quaterniond()));
        ShaderInstance shader = texturedShader;
        shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);

        CelestialPlacement placement = CelestialPlacement.of(context);
        ResourceLocation id = registry.getKey(toRender);
        renderCubeAt(id, shadows, context.poseStack(), placement.position(), placement.halfExtent(), placement.rotation());
    }

    /// The volumes that other celestials shade, where they could reach the cube
    private static List<ShadowVolume> computeShadows(Registry<Celestial> registry, Celestial toRender, Vector3dc position,
                                                     Quaterniondc rotation, Vector3dc starPosition, Time time) {
        OBB selfOBB = OBB.createCube(toRender.size(), rotation, position);
        return ShadowVolume.compute(selfOBB, CelestialShadows.casters(registry, toRender, time), starPosition);
    }

    private static void renderCubeAt(ResourceLocation planetID, List<ShadowVolume> shadows, PoseStack poseStack,
                                     Vector3dc position, double halfExtent, Quaterniondc localRotation) {
        // Get the texture for this planet
        ResourceLocation textureLocation = textureLocation(planetID);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderType = getTexturedRenderType(textureLocation);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        Matrix4f matrix = new Matrix4f(poseStack.last().pose())
                .translate((float) position.x(), (float) position.y(), (float) position.z());
        Quaternionf rotation = new Quaternionf(localRotation);
        matrix.rotate(rotation);

        float halfSize = (float) halfExtent;

        // Laid out in the texture as CubeFace has them, as the texture generator draws them too
        for (CubeFace face : CubeFace.values()) {
            addTexturedCubeFace(matrix, buffer, rotation,
                    face.point(0, 1).mul(halfSize), face.point(1, 1).mul(halfSize),
                    face.point(1, 0).mul(halfSize), face.point(0, 0).mul(halfSize),
                    face.u0(), face.v0(), face.u1(), face.v1());
        }

        setShadowUniforms(shadows, matrix);

        // End batch to flush planet rendering
        bufferSource.endBatch(renderType);
        bufferSource.endBatch();
    }

    /// A face from its corners: bottom left, bottom right, top right and top left as the texture has them
    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, Quaternionf rotation,
                                            Vector3f bottomLeft, Vector3f bottomRight, Vector3f topRight, Vector3f topLeft,
                                            float u1, float v1, float u2, float v2) {
        addTexturedVertexWithLighting(matrix, buffer, bottomLeft.x, bottomLeft.y, bottomLeft.z, u1, v2, rotation);
        addTexturedVertexWithLighting(matrix, buffer, bottomRight.x, bottomRight.y, bottomRight.z, u2, v2, rotation);
        addTexturedVertexWithLighting(matrix, buffer, topRight.x, topRight.y, topRight.z, u2, v1, rotation);
        addTexturedVertexWithLighting(matrix, buffer, topLeft.x, topLeft.y, topLeft.z, u1, v1, rotation);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Quaternionf rotation) {
        Vector3f rotatedNormal = rotation.transform(new Vector3f(x, y, z).normalize());

        buffer.vertex(matrix, x, y, z)
                .uv(u, v)
                .color(255, 255, 255, 255)
                .normal(rotatedNormal.x, rotatedNormal.y, rotatedNormal.z)
                .endVertex();
    }

    /// Tells the cube's shader what shades it, for the cube that's about to be drawn with {@code matrix}
    private static void setShadowUniforms(List<ShadowVolume> shadows, Matrix4f matrix) {
        ShaderInstance shader = texturedShader;

        // The shader works in the space the vertices are uploaded in, which is the cube's local space transformed by
        // the matrix
        Vector3f origin = matrix.transformPosition(new Vector3f());
        shader.safeGetUniform("ShadowOrigin").set(origin.x, origin.y, origin.z);
        CelestialShadows.setUniforms(shader, shadows, matrix);
    }

    /// If a texture with the {@code _emissive} suffix exists next to the cube's texture, it is bound to Sampler1 and
    /// rendered unaffected by lighting.
    ///
    /// @param textureLocation the texture location, without the {@code textures/} prefix or {@code .png} extension
    private static RenderType getTexturedRenderType(ResourceLocation textureLocation) {
        return TEXTURED_RENDER_TYPES.computeIfAbsent(textureLocation, loc -> {
            ResourceLocation fullTexturePath = loc.withPath(path -> "textures/" + path + ".png");
            ResourceLocation emissiveTexturePath = loc.withPath(path -> "textures/" + path + "_emissive.png");
            if (!TextureSlot.exists(emissiveTexturePath)) {
                emissiveTexturePath = NO_EMISSIVE_TEXTURE;
            }
            return RenderTypes.textured(loc, fullTexturePath, emissiveTexturePath);
        });
    }

    // Vanilla render types rather than Lodestone's: those never write depth outside Fabulous graphics, which celestials
    // need to hide the stars behind them
    private static final class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        static RenderType textured(ResourceLocation id, ResourceLocation texture, ResourceLocation emissiveTexture) {
            return create("genesis_shaded_cube_" + id.getNamespace() + "_" + id.getPath().replace("/", "_"),
                    DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                            .setShaderState(new ShaderStateShard(() -> texturedShader))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(CULL)
                            .setTextureState(MultiTextureStateShard.builder()
                                    .add(texture, false, false)
                                    .add(emissiveTexture, false, false)
                                    .build())
                            .createCompositeState(false));
        }
    }

    public static final Codec<ShadedCubeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MoreCodecs.bool(true).fieldOf("drawShadows").forGetter(ShadedCubeComponent::drawShadows)
            ).apply(instance, ShadedCubeComponent::new)
    );
}
