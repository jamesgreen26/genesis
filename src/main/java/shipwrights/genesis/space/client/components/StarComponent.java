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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;

import java.io.IOException;

/// A glowing star, fading from {@code color0} at its edge through {@code color1} to white at its core. Colours are
/// packed {@code 0xRRGGBB} values.
///
/// Drawn as a cube whose shader traces the view ray through it; each vertex's colour carries its position within the
/// cube.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record StarComponent(
        int color0,
        int color1
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("star");

    private static ShaderInstance sunShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("sun"),
                DefaultVertexFormat.POSITION_COLOR), shader -> sunShader = shader);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public boolean shinesThroughSky() {
        return true;
    }

    @Override
    public void render(CelestialRenderContext context) {
        CelestialPlacement placement = CelestialPlacement.of(context);
        float opacity = placement.alpha() * context.weatherVisibility();
        if (opacity < 0.01f) return;

        Quaternionf rotation = new Quaternionf(placement.rotation());
        Vector3f position = new Vector3f((float) placement.position().x(), (float) placement.position().y(),
                (float) placement.position().z());
        float halfSize = (float) placement.halfExtent();

        // The camera in the star's own rotated space
        Vector3f cameraPosition = rotation.conjugate(new Quaternionf()).transform(position.negate(new Vector3f()));

        ShaderInstance shader = sunShader;
        shader.safeGetUniform("CameraPosition").set(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        shader.safeGetUniform("HalfSize").set(halfSize);
        shader.safeGetUniform("Color0").set(red(color0), green(color0), blue(color0));
        shader.safeGetUniform("Color1").set(red(color1), green(color1), blue(color1));
        shader.safeGetUniform("Opacity").set(opacity);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.STAR);

        Matrix4f matrix = new Matrix4f(context.poseStack().last().pose())
                .translate(position)
                .rotate(rotation);

        addCubeFace(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFace(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFace(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);

        // Flush now, while the uniforms are still this star's
        bufferSource.endBatch(RenderTypes.STAR);
    }

    private static void addCubeFace(Matrix4f matrix, VertexConsumer buffer, float x4, float y4, float z4, float x3, float y3, float z3,
                                    float x2, float y2, float z2, float x1, float y1, float z1) {
        float halfSize = Math.abs(x1);
        addVertex(matrix, buffer, halfSize, x1, y1, z1);
        addVertex(matrix, buffer, halfSize, x2, y2, z2);
        addVertex(matrix, buffer, halfSize, x3, y3, z3);
        addVertex(matrix, buffer, halfSize, x4, y4, z4);
    }

    /// The colour is the vertex's position within the cube, from 0 at {@code -halfSize} to 255 at {@code halfSize}
    private static void addVertex(Matrix4f matrix, VertexConsumer buffer, float halfSize, float x, float y, float z) {
        float size = 2 * halfSize;
        buffer.vertex(matrix, x, y, z)
                .color((int) (255 * (x + halfSize) / size), (int) (255 * (y + halfSize) / size), (int) (255 * (z + halfSize) / size), 255)
                .endVertex();
    }

    private static float red(int color) {
        return (color >> 16 & 0xFF) / 255f;
    }

    private static float green(int color) {
        return (color >> 8 & 0xFF) / 255f;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255f;
    }

    // Vanilla render types rather than Lodestone's, like ShadedCubeComponent's
    private static final class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        // Additive, like vanilla's sun, so the sky behind it brightens it towards white. The shader's own blend mode
        // matches, since applying the shader applies that instead.
        // Doesn't write depth: the glow fades out towards the cube's edges, which shouldn't hide what's behind them
        static final RenderType STAR = create("genesis_star", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> sunShader))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(CULL)
                        .createCompositeState(false));
    }

    public static final Codec<StarComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("color0").forGetter(StarComponent::color0),
                    Codec.INT.fieldOf("color1").forGetter(StarComponent::color1)
            ).apply(instance, StarComponent::new)
    );
}
