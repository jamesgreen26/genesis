package shipwrights.genesis.space.client.components;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Daylight;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.shading.FaceShadow;
import shipwrights.genesis.space.client.shading.ShadowProjection;
import shipwrights.genesis.space.client.shading.ShadowRenderer;
import shipwrights.genesis.space.math.AAPlane;
import shipwrights.genesis.space.math.OBB;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/// A textured cube lit by the nearest light-casting celestial, optionally with shadows cast by other celestials.
///
/// The texture is {@code genesis:celestials/<namespace>/<path>}, named after the celestial's id.
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public record ShadedCubeComponent(
        boolean drawShadows
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("shaded_cube");

    private static ShaderInstance texturedShader;
    private static ShaderInstance shadowShader;

    // Bound in place of the emissive texture when a planet doesn't have one, so it emits nothing
    private static final ResourceLocation NO_EMISSIVE_TEXTURE = GenesisMod.id("textures/celestials/no_emissive.png");

    private static final ConcurrentHashMap<ResourceLocation, RenderType> TEXTURED_RENDER_TYPES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("planet_textured"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), shader -> texturedShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("planet_shadow"),
                DefaultVertexFormat.POSITION_COLOR), shader -> shadowShader = shader);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        // Textured render types check whether an emissive texture exists, so must be rebuilt when resource packs change
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> TEXTURED_RENDER_TYPES.clear());
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

        Registry<Celestial> registry = context.clientLevel().registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Vector3dc position = toRender.transformProvider().getPosition(registry, time);
        Quaterniondc rotation = toRender.transformProvider().getRotation(registry, time);

        // Nothing to light the cube with
        Optional<Vector3dc> starPosition = Daylight.nearestLightSource(registry, toRender, position, time);
        if (starPosition.isEmpty()) return;

        List<FaceShadow> shadows = drawShadows
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

    private static List<FaceShadow> computeShadows(Registry<Celestial> registry, Celestial toRender, Vector3dc position,
                                                   Quaterniondc rotation, Vector3dc starPosition, Time time) {
        OBB selfOBB = OBB.createCube(toRender.size(), rotation, position);
        List<OBB> otherOBBs = registry.stream()
                .filter(it -> it.castsShadows() && it != toRender)
                .map(it -> OBB.createCube(
                        it.size(),
                        it.transformProvider().getRotation(registry, time),
                        it.transformProvider().getPosition(registry, time)
                ))
                .toList();

        return ShadowProjection.computeShadows(selfOBB, otherOBBs, starPosition);
    }

    private static void renderCubeAt(ResourceLocation planetID, List<FaceShadow> shadows, PoseStack poseStack,
                                     Vector3dc position, double halfExtent, Quaterniondc localRotation) {
        // Get the texture for this planet
        ResourceLocation textureLocation = GenesisMod.id("celestials/" + planetID.getNamespace() + "/" + planetID.getPath());

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderType = getTexturedRenderType(textureLocation);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        Matrix4f matrix = new Matrix4f(poseStack.last().pose())
                .translate((float) position.x(), (float) position.y(), (float) position.z());
        Quaternionf rotation = new Quaternionf(localRotation);
        matrix.rotate(rotation);

        float halfSize = (float) halfExtent;

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        addTexturedCubeFace(matrix, buffer, rotation, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, twoThirds, 0.0f, 1.0f, 0.5f);        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, rotation, halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0f, 0.0f, third, 0.5f);        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, rotation, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, third, 0.0f, twoThirds, 0.5f);       // West face (-X)
        addTexturedCubeFace(matrix, buffer, rotation, halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 0.0f, 0.5f, third, 1.0f);            // East face (+X)
        addTexturedCubeFace(matrix, buffer, rotation, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, third, 0.5f, twoThirds, 1.0f);       // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, rotation, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, twoThirds, 0.5f, 1.0f, 1.0f);        // Up face (+Y)

        // End batch to flush planet rendering
        bufferSource.endBatch(renderType);
        bufferSource.endBatch();

        renderShadows(shadows, matrix, halfExtent);
        bufferSource.endBatch();
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, Quaternionf rotation,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            float u1, float v1, float u2, float v2) {
        addTexturedVertexWithLighting(matrix, buffer, x1, y1, z1, u1, v2, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x2, y2, z2, u2, v2, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x3, y3, z3, u2, v1, rotation);
        addTexturedVertexWithLighting(matrix, buffer, x4, y4, z4, u1, v1, rotation);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Quaternionf rotation) {
        Vector3f rotatedNormal = rotation.transform(new Vector3f(x, y, z).normalize());

        buffer.addVertex(matrix, x, y, z)
                .setUv(u, v)
                .setColor(255, 255, 255, 255)
                .setNormal(rotatedNormal.x, rotatedNormal.y, rotatedNormal.z);
    }

    private static void renderShadows(List<FaceShadow> shadows, Matrix4f matrix, double halfExtent) {
        if (shadows.isEmpty()) {
            return;
        }

        // Use a single edgeWidth across all face shadows so that the soft
        // falloff scale is consistent across cube edges.
        float sharedEdgeWidth = 0.0001f;
        for (FaceShadow shadow : shadows) {
            sharedEdgeWidth = Math.max(sharedEdgeWidth, ShadowRenderer.computeEdgeWidth(shadow));
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // Render each shadow in its own batch so we can upload
        // per-shadow projection vertices to the shader.
        for (FaceShadow shadow : shadows) {
            VertexConsumer shadowBuffer = bufferSource.getBuffer(RenderTypes.SHADOW);

            // Compute up to 8 vertices for this shadow from the projected
            // polygon, transform them with the same matrix used for
            // rendering, and upload them as uniforms. This keeps the
            // shader's ShadowVertexN positions in the same space as the
            // Position/localPos attribute.
            ShaderInstance shader = shadowShader;
            if (shader != null) {
                List<Vector2dc> polygon = shadow.polygon();
                AAPlane plane = shadow.plane();

                int count = Math.min(polygon.size(), 8);

                Uniform countUniform = shader.getUniform("ShadowVertexCount");
                if (countUniform != null) {
                    countUniform.set((float) count);
                }

                // Build a bitmask of edges that lie on cube face clipping boundaries.
                // These are artifacts of Sutherland-Hodgman clipping, not real shadow
                // silhouette edges, so they must not contribute to the fade falloff.
                int boundaryMask = 0;
                double eps = halfExtent * 1e-4;
                for (int i = 0; i < count; i++) {
                    int j = (i + 1) % count;
                    Vector2dc a = polygon.get(i);
                    Vector2dc b = polygon.get(j);
                    boolean onBoundary =
                            (Math.abs(a.x() - halfExtent) < eps && Math.abs(b.x() - halfExtent) < eps) ||
                            (Math.abs(a.x() + halfExtent) < eps && Math.abs(b.x() + halfExtent) < eps) ||
                            (Math.abs(a.y() - halfExtent) < eps && Math.abs(b.y() - halfExtent) < eps) ||
                            (Math.abs(a.y() + halfExtent) < eps && Math.abs(b.y() + halfExtent) < eps);
                    if (onBoundary) {
                        boundaryMask |= (1 << i);
                    }
                }
                Uniform maskUniform = shader.getUniform("ShadowEdgeMask");
                if (maskUniform != null) {
                    maskUniform.set((float) boundaryMask);
                }

                Uniform edgeWidthUniform = shader.getUniform("ShadowEdgeWidth");
                if (edgeWidthUniform != null) {
                    edgeWidthUniform.set(sharedEdgeWidth);
                }

                for (int i = 0; i < 8; i++) {
                    Uniform u = shader.getUniform("ShadowVertex[" + i + "]");
                    if (u != null) {
                        if (i < count) {
                            // Start from local-space projection and apply
                            // the same z-fighting offset and matrix that
                            // the geometry uses.
                            Vector3d v3d = ShadowRenderer.applyZFightingOffset(
                                    ShadowRenderer.convertPlaneToLocal3D(polygon.get(i), plane),
                                    plane,
                                    halfExtent
                            );

                            Vector3f transformed = new Vector3f((float) v3d.x, (float) v3d.y, (float) v3d.z);
                            matrix.transformPosition(transformed);

                            u.set(transformed.x, transformed.y, transformed.z);
                        } else {
                            u.set(0.0F, 0.0F, 0.0F);
                        }
                    }
                }
            }

            ShadowRenderer.renderShadow(shadow, matrix, shadowBuffer, halfExtent);

            // Flush this shadow's batch so its uniforms apply only to it
            bufferSource.endBatch(RenderTypes.SHADOW);
            bufferSource.endBatch();
        }
    }

    /// If a texture with the {@code _emissive} suffix exists next to the cube's texture, it is bound to Sampler1 and
    /// rendered unaffected by lighting.
    ///
    /// @param textureLocation the texture location, without the {@code textures/} prefix or {@code .png} extension
    private static RenderType getTexturedRenderType(ResourceLocation textureLocation) {
        return TEXTURED_RENDER_TYPES.computeIfAbsent(textureLocation, loc -> {
            ResourceLocation fullTexturePath = loc.withPath(path -> "textures/" + path + ".png");
            ResourceLocation emissiveTexturePath = loc.withPath(path -> "textures/" + path + "_emissive.png");
            if (Minecraft.getInstance().getResourceManager().getResource(emissiveTexturePath).isEmpty()) {
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

        static final RenderType SHADOW = create("genesis_shaded_cube_shadow", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES, 256, false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> shadowShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(CULL)
                        .createCompositeState(false));

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

    public static final MapCodec<ShadedCubeComponent> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("drawShadows").forGetter(ShadedCubeComponent::drawShadows)
            ).apply(instance, ShadedCubeComponent::new)
    );
}
