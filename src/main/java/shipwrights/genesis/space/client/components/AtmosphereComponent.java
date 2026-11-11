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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;

import java.io.IOException;
import java.util.Optional;

/// A haze around a cube-shaped celestial, lit by the nearest light-casting celestial: blue on the day side, reddening
/// towards the terminator and dim purple at night.
///
/// Drawn as a shell whose shader traces the view ray through it; each vertex's colour carries its position within the
/// atmosphere.
///
/// @param density   opacity of the haze, from 0 for none
/// @param thickness how far the atmosphere extends beyond the surface, in units of 30% of the celestial's half size
/// @param color     packed {@code 0xRRGGBB}; not used yet, the shader has fixed day, sunset and night colours
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record AtmosphereComponent(
        double density,
        double thickness,
        int color
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("atmosphere");

    /// Heights the air thins out between, as in the 1.20.1 branch, where the end was the configurable atmosphere entry
    /// height
    private static final double FADE_START_Y = 320.0;
    private static final double FADE_END_Y = 1440.0;

    private static ShaderInstance atmosphereShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("planet_atmosphere"),
                DefaultVertexFormat.POSITION_COLOR), shader -> atmosphereShader = shader);
    }

    /// How far the camera at {@code cameraY} has climbed out of the atmosphere of the celestial it's standing on, from 0
    /// near the surface to 1 above it
    private static float altitudeFade(double cameraY) {
        return (float) Mth.clamp((cameraY - FADE_START_Y) / (FADE_END_Y - FADE_START_Y), 0.0, 1.0);
    }

    /// How dense the air around the camera is, standing on the celestial {@code celestialId} at {@code cameraY}: its
    /// atmosphere's density, capped at 1, near the surface, thinning to none as the camera climbs out of it. None if the
    /// celestial has no atmosphere.
    public static float airDensity(ResourceLocation celestialId, double cameraY) {
        float density = CelestialVisualizer.REGISTRY.registry().getOptional(celestialId).stream()
                .flatMap(visualizer -> visualizer.components().stream())
                .filter(AtmosphereComponent.class::isInstance)
                .map(component -> (float) ((AtmosphereComponent) component).density())
                .findFirst()
                .orElse(0f);
        return Mth.clamp(density, 0f, 1f) * (1f - altitudeFade(cameraY));
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void render(CelestialRenderContext context) {
        CelestialPlacement placement = CelestialPlacement.of(context);
        if (density < 0.01) return;

        Registry<Celestial> registry = context.clientLevel().registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Vector3dc position = context.toRender().transformProvider().getPosition(registry, context.time());
        Optional<Vector3dc> lightSource = ShadedCubeComponent.nearestLightSource(registry, context.toRender(), position, context.time());
        if (lightSource.isEmpty()) return;

        Quaternionf rotation = new Quaternionf(placement.rotation());
        Vector3f center = new Vector3f((float) placement.position().x(), (float) placement.position().y(),
                (float) placement.position().z());
        float halfSize = (float) placement.halfExtent();
        float relativeSize = 1.0f + 0.3f * (float) thickness;

        // The shader works in the celestial's own rotated space, so the camera and light are given in it too. The light
        // direction goes from the observer's frame, as for ShadedCubeComponent, into the placement's rotation.
        Vector3f cameraPosition = rotation.conjugate(new Quaternionf()).transform(center.negate(new Vector3f()));
        Vector3d lightDirection = new Vector3d(position).sub(lightSource.get())
                .rotate(context.vantagePoint().getRotation().conjugate(new Quaterniond()))
                .rotate(placement.rotation().conjugate(new Quaterniond()));

        ShaderInstance shader = atmosphereShader;
        shader.safeGetUniform("CameraPosition").set(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        shader.safeGetUniform("LightDirection").set((float) lightDirection.x, (float) lightDirection.y, (float) lightDirection.z);
        shader.safeGetUniform("HalfSize").set(halfSize);
        shader.safeGetUniform("AtmosphereThickness").set(relativeSize);
        shader.safeGetUniform("Density").set((float) density);

        float limit = halfSize * relativeSize;
        boolean inside = Math.abs(cameraPosition.x) < limit && Math.abs(cameraPosition.y) < limit
                && Math.abs(cameraPosition.z) < limit;

        // From outside, the shell is drawn at the atmosphere's edge, with the inner shell collapsed to nothing. From
        // inside, the near shell hugs the surface and the far one, mirrored so it faces inwards, is at the edge.
        float nearScale = inside ? 1.001f : relativeSize;
        float farScale = -relativeSize * (inside ? 1.0f : 1e-9f);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.ATMOSPHERE);

        Matrix4f matrix = new Matrix4f(context.poseStack().last().pose())
                .translate(center)
                .rotate(rotation);

        float h = halfSize;
        addFace(matrix, buffer, -h, -h, h, h, -h, h, h, h, h, -h, h, h, h, nearScale, farScale, relativeSize);
        addFace(matrix, buffer, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -h, h, nearScale, farScale, relativeSize);
        addFace(matrix, buffer, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, h, nearScale, farScale, relativeSize);
        addFace(matrix, buffer, h, -h, -h, h, h, -h, h, h, h, h, -h, h, h, nearScale, farScale, relativeSize);
        addFace(matrix, buffer, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, h, nearScale, farScale, relativeSize);
        addFace(matrix, buffer, -h, h, -h, -h, h, h, h, h, h, h, h, -h, h, nearScale, farScale, relativeSize);

        // Flush now, while the uniforms are still this celestial's
        bufferSource.endBatch(RenderTypes.ATMOSPHERE);
    }

    /// One face of the near shell and the matching face of the far shell, each scaled from the cube's corners
    private static void addFace(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4, float halfSize,
                                float nearScale, float farScale, float relativeSize) {
        for (float scale : new float[]{nearScale, farScale}) {
            addVertex(matrix, buffer, halfSize, relativeSize, x1 * scale, y1 * scale, z1 * scale);
            addVertex(matrix, buffer, halfSize, relativeSize, x2 * scale, y2 * scale, z2 * scale);
            addVertex(matrix, buffer, halfSize, relativeSize, x3 * scale, y3 * scale, z3 * scale);
            addVertex(matrix, buffer, halfSize, relativeSize, x4 * scale, y4 * scale, z4 * scale);
        }
    }

    /// The colour is the vertex's position within the atmosphere, from 0 at its {@code -X/Y/Z} edge to 255 at its
    /// {@code +X/Y/Z} edge
    private static void addVertex(Matrix4f matrix, VertexConsumer buffer, float halfSize, float relativeSize,
                                  float x, float y, float z) {
        float size = 2 * halfSize;
        buffer.vertex(matrix, x, y, z)
                .color((int) (255 * (x / relativeSize + halfSize) / size), (int) (255 * (y / relativeSize + halfSize) / size),
                        (int) (255 * (z / relativeSize + halfSize) / size), 255)
                .endVertex();
    }

    // Vanilla render types rather than Lodestone's, like ShadedCubeComponent's
    private static final class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        // Doesn't write depth: it's translucent, and shouldn't hide what's behind it
        static final RenderType ATMOSPHERE = create("genesis_atmosphere", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> atmosphereShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(CULL)
                        .createCompositeState(false));
    }

    public static final Codec<AtmosphereComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("density").forGetter(AtmosphereComponent::density),
                    Codec.DOUBLE.fieldOf("thickness").forGetter(AtmosphereComponent::thickness),
                    Codec.INT.fieldOf("color").forGetter(AtmosphereComponent::color)
            ).apply(instance, AtmosphereComponent::new)
    );
}
