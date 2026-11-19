package shipwrights.genesis.space.client.components;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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
import net.minecraftforge.api.distmarker.Dist;
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
import org.joml.primitives.AABBd;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Daylight;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.client.BlendedShaderInstance;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.TextureSlot;
import shipwrights.genesis.space.client.shading.CelestialShadows;
import shipwrights.genesis.space.client.shading.ShadowVolume;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.util.MoreCodecs;
import shipwrights.genesis.util.schema.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// A flat, square ring around one of the axes of a cube-shaped celestial, turning with it. The celestial and others
/// cast shadows onto it; it casts none itself, and hides nothing that's behind it.
///
/// The texture is {@code genesis:textures/celestials/<namespace>/<path>_ring.png}, named after the celestial's id. It's a slice of
/// the ring, with the ring's outer edge at the top and its inner edge at the bottom, repeated along each side of the
/// ring from the middle of the side. Where two sides meet at a corner, each is cut off at the diagonal.
///
/// List it after the celestial's {@link ShadedCubeComponent}, so that it's drawn over the cube where it passes in
/// front.
///
/// @param innerSize the length of the sides of the ring's inner edge, as a multiple of the celestial's size
/// @param outerSize the length of the sides of the ring's outer edge, as a multiple of the celestial's size
/// @param axis      which of the celestial's own axes the ring is around. Around {@code y}, as it is unless told
///                  otherwise, it's in the plane of the celestial's equator.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record TexturedRingComponent(
        double innerSize,
        double outerSize,
        Axis axis
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("textured_ring");

    /// An axis of the celestial a ring can be around, in the celestial's local space
    public enum Axis {
        X, Y, Z;

        static final Codec<Axis> CODEC = MoreCodecs.oneOf("x", "y", "z").xmap(
                name -> valueOf(name.toUpperCase(Locale.ROOT)),
                axis -> axis.name().toLowerCase(Locale.ROOT));

        /// Where a point in the plane of a ring around this axis is, in the celestial's local space. The plane's
        /// two directions are the same two turns on from each axis, so that a ring looks the same from the positive
        /// end of whichever axis it's around.
        Vector3f place(float u, float v) {
            return switch (this) {
                case X -> new Vector3f(0, v, u);
                case Y -> new Vector3f(u, 0, v);
                case Z -> new Vector3f(v, u, 0);
            };
        }
    }

    private static ShaderInstance ringShader;

    private static final ConcurrentHashMap<ResourceLocation, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new BlendedShaderInstance(event.getResourceProvider(), GenesisMod.id("textured_ring"),
                DefaultVertexFormat.POSITION_TEX, PremultipliedTransparency.TRANSLUCENT::setupRenderState), shader -> ringShader = shader);
    }

    /// The ring's texture for the celestial {@code id}
    private static ResourceLocation textureLocation(ResourceLocation id) {
        return GenesisMod.id("textures/celestials/" + id.getNamespace() + "/" + id.getPath() + "_ring.png");
    }

    @Override
    public List<TextureSlot> textures(ResourceLocation celestialId) {
        return List.of(new TextureSlot(textureLocation(celestialId),
                Component.translatable("genesis.texture.textured_ring"), true,
                Component.translatable("genesis.texture.textured_ring.hint")));
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void render(CelestialRenderContext context) {
        // Nothing to draw between the edges
        if (outerSize <= innerSize) return;

        Celestial toRender = context.toRender();
        Time time = context.time();
        Registry<Celestial> registry = context.celestials();
        Vector3dc position = toRender.transformProvider().getPosition(registry, time);
        Quaterniondc rotation = toRender.transformProvider().getRotation(registry, time);

        // Nothing to light the ring with
        Optional<Vector3dc> starPosition = Daylight.nearestLightSource(registry, toRender, position, time);
        if (starPosition.isEmpty()) return;

        CelestialPlacement placement = CelestialPlacement.of(context);
        float halfSize = (float) placement.halfExtent();
        float inner = (float) (innerSize * placement.halfExtent());
        float outer = (float) (outerSize * placement.halfExtent());

        // The shader works in the celestial's local space, which the ring's vertices are left in
        Matrix4f localToDrawn = new Matrix4f(context.poseStack().last().pose())
                .translate((float) placement.position().x(), (float) placement.position().y(), (float) placement.position().z())
                .rotate(new Quaternionf(placement.rotation()));
        // The camera is where celestials are placed relative to
        Vector3d cameraPosition = placement.rotation().conjugate(new Quaterniond())
                .transform(placement.position().negate(new Vector3d()));

        ShaderInstance shader = ringShader;
        shader.safeGetUniform("LocalMat").set(localToDrawn);
        shader.safeGetUniform("CameraPosition").set((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
        shader.safeGetUniform("HalfSize").set(halfSize);
        CelestialShadows.setUniforms(shader,
                computeShadows(registry, toRender, position, rotation, starPosition.get(), time), new Matrix4f());

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType renderType = RENDER_TYPES.computeIfAbsent(
                textureLocation(registry.getKey(toRender)), RenderTypes::ring);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        // The direction along each side, and outwards from it, in the ring's plane. Around the Y axis, that's
        // clockwise seen from above, from the north side.
        addSide(buffer, inner, outer, 1, 0, 0, -1);
        addSide(buffer, inner, outer, 0, 1, 1, 0);
        addSide(buffer, inner, outer, -1, 0, 0, 1);
        addSide(buffer, inner, outer, 0, -1, -1, 0);

        // Flush now, while the uniforms are still this celestial's
        bufferSource.endBatch(renderType);
    }

    /// The volumes shaded by the celestial the ring is around, and by any others that could reach the ring
    private List<ShadowVolume> computeShadows(Registry<Celestial> registry, Celestial toRender, Vector3dc position,
                                              Quaterniondc rotation, Vector3dc starPosition, Time time) {
        // Flat along the axis it's around
        Vector3d extent = new Vector3d(outerSize * toRender.size() / 2).setComponent(axis.ordinal(), 0);
        OBB ring = new OBB(new AABBd(extent.negate(new Vector3d()), extent), rotation, position);

        List<ShadowVolume> volumes = new ArrayList<>();
        if (toRender.castsShadows()) {
            // Never found among the others' shadows, which only looks for what's nearer the light than the ring is
            ShadowVolume own = ShadowVolume.cast(ring, OBB.createCube(toRender.size(), rotation, position), starPosition);
            if (own != null) volumes.add(own);
        }
        volumes.addAll(ShadowVolume.compute(ring, CelestialShadows.casters(registry, toRender, time), starPosition));
        return volumes;
    }

    /// One side of the ring: the strip between its inner and outer edges, cut off at the diagonals where it meets
    /// the sides next to it.
    ///
    /// @param alongU with {@code alongV}, the direction in the ring's plane that the texture repeats in along the side
    /// @param outU   with {@code outV}, the direction in the ring's plane from the celestial to the side
    private void addSide(VertexConsumer buffer, float inner, float outer,
                         int alongU, int alongV, int outU, int outV) {
        float width = outer - inner;
        addVertex(buffer, -outer, outer, alongU, alongV, outU, outV, width, 0);
        addVertex(buffer, outer, outer, alongU, alongV, outU, outV, width, 0);
        addVertex(buffer, inner, inner, alongU, alongV, outU, outV, width, 1);
        addVertex(buffer, -inner, inner, alongU, alongV, outU, outV, width, 1);
    }

    /// @param along how far along the side the vertex is from the middle of the side
    /// @param out   how far out from the celestial it is
    /// @param v     0 at the ring's outer edge, 1 at its inner edge
    private void addVertex(VertexConsumer buffer, float along, float out,
                           int alongU, int alongV, int outU, int outV, float width, float v) {
        Vector3f position = axis.place(along * alongU + out * outU, along * alongV + out * outV);
        // The shader scales the distance along the side by the texture's shape, to keep its pixels square
        buffer.vertex(position.x, position.y, position.z)
                .uv(along / width, v)
                .endVertex();
    }

    // Vanilla render types rather than Lodestone's, like ShadedCubeComponent's
    private static final class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        // Doesn't write depth: it's translucent, and shouldn't hide what's behind it. Seen from both sides.
        static RenderType ring(ResourceLocation texture) {
            return create("genesis_textured_ring_" + texture.getPath().replace("/", "_"),
                    DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                            .setShaderState(new ShaderStateShard(() -> ringShader))
                            .setTransparencyState(PremultipliedTransparency.TRANSLUCENT)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .createCompositeState(false));
        }
    }

    public static final Codec<TexturedRingComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MoreCodecs.doubleRange(Schema.Range.above(0), 1.5).fieldOf("innerSize").forGetter(TexturedRingComponent::innerSize),
                    MoreCodecs.doubleRange(Schema.Range.above(0), 2.5).fieldOf("outerSize").forGetter(TexturedRingComponent::outerSize),
                    MoreCodecs.strictOptionalField(Axis.CODEC, "axis", Axis.Y).forGetter(TexturedRingComponent::axis)
            ).apply(instance, TexturedRingComponent::new)
    );
}
