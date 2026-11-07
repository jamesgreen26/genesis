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
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.client.CelestialPlacement;
import shipwrights.genesis.space.client.CelestialRenderContext;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.util.MoreCodecs;

import java.io.IOException;

/// A soft glow around a star, fading out from its centre. In space it fades away as the observer nears the star, where
/// the glow would cover the whole sky.
///
/// Drawn additively as a disc facing the camera, like vanilla's sun, so against a daytime sky it brightens towards a
/// white disc around the star. Seen from a planet, it blends from its look in space under a dark sky to its own colour
/// and opacity under a bright one, strong enough to show against it. Fades with the weather like other celestials.
/// List it before the star's own component so the star is drawn over it.
///
/// @param color        packed {@code 0xRRGGBB}, in space and under a dark sky
/// @param radius       radius of the glow around the star, in blocks
/// @param opacity      opacity at the centre of the glow, in space and under a dark sky
/// @param fadeDistance distance from the star within which the glow fades out, in blocks
/// @param skyColor     packed {@code 0xRRGGBB}, under a bright sky
/// @param skyOpacity   opacity at the centre of the glow, under a bright sky
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record StarGlowComponent(
        int color,
        double radius,
        double opacity,
        double fadeDistance,
        int skyColor,
        double skyOpacity
) implements CelestialVisualizer.Component {
    public static final ResourceLocation TYPE = GenesisMod.id("star_glow");

    /// How far from the camera the disc is drawn. Its depth is set by the dispatcher, so this only needs to be inside
    /// the view distance.
    private static final double DRAW_DISTANCE = 100.0;

    private static final int SEGMENTS = 32;

    private static ShaderInstance glowShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), GenesisMod.id("star_glow"),
                DefaultVertexFormat.POSITION_COLOR), shader -> glowShader = shader);
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
        boolean onPlanet = context.vantagePoint() instanceof VantagePoint.OnCelestial;
        // Standing on a star isn't a view of it
        if (onPlanet && ((VantagePoint.OnCelestial) context.vantagePoint()).celestial() == context.toRender()) return;

        Vector3d toStar = new Vector3d(CelestialPlacement.of(context).position());
        double distance = toStar.length();
        if (distance < 1e-6) return;

        float alpha = (float) (opacity * Mth.clamp(distance / fadeDistance, 0.0, 1.0));
        float r = red(color), g = green(color), b = blue(color);

        float skyBrightness = onPlanet
                ? CelestialPlacement.skyBrightness(context.clientLevel(), context.camera().getPosition(), context.time().partialTick())
                : 0f;
        if (skyBrightness > 0f) {
            alpha = Mth.lerp(skyBrightness, alpha, (float) skyOpacity);
            r = Mth.lerp(skyBrightness, r, red(skyColor));
            g = Mth.lerp(skyBrightness, g, green(skyColor));
            b = Mth.lerp(skyBrightness, b, blue(skyColor));
        }
        alpha *= context.weatherVisibility();
        if (alpha <= 0.001f) return;

        // A disc around the direction of the star, scaled to cover the glow's radius at the star's distance
        toStar.div(distance);
        Vector3d up = Math.abs(toStar.y) > 0.98 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = toStar.cross(up, new Vector3d()).normalize();
        Vector3d upPerp = right.cross(toStar, new Vector3d()).normalize();
        Vector3d center = new Vector3d(toStar).mul(DRAW_DISTANCE);
        double discRadius = radius * DRAW_DISTANCE / distance;

        glowShader.safeGetUniform("BaseAlpha").set(alpha);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.GLOW);
        Matrix4f matrix = context.poseStack().last().pose();

        // The shader takes vertex alpha as the distance from the centre, 0 to 1
        Vector3d previous = rimPoint(center, right, upPerp, discRadius, 0);
        for (int i = 1; i <= SEGMENTS; i++) {
            Vector3d next = rimPoint(center, right, upPerp, discRadius, i);
            buffer.vertex(matrix, (float) center.x, (float) center.y, (float) center.z).color(r, g, b, 0f).endVertex();
            buffer.vertex(matrix, (float) previous.x, (float) previous.y, (float) previous.z).color(r, g, b, 1f).endVertex();
            buffer.vertex(matrix, (float) next.x, (float) next.y, (float) next.z).color(r, g, b, 1f).endVertex();
            previous = next;
        }

        // Flush now, while the uniforms are still this star's
        bufferSource.endBatch(RenderTypes.GLOW);
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

    private static Vector3d rimPoint(Vector3d center, Vector3d right, Vector3d up, double radius, int segment) {
        double angle = segment * 2.0 * Math.PI / SEGMENTS;
        return new Vector3d(center)
                .fma(Math.cos(angle) * radius, right)
                .fma(Math.sin(angle) * radius, up);
    }

    // Vanilla render types rather than Lodestone's, like ShadedCubeComponent's
    private static final class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        // Additive, like vanilla's sun and StarComponent: over black space that's the same as blending it over, and
        // over the sky it brightens towards white. The shader's own blend mode matches, since applying the shader
        // applies that instead.
        // Doesn't write depth, so it doesn't hide what's behind it. Depth tested, so celestials in front of the star
        // hide the glow too.
        static final RenderType GLOW = create("genesis_star_glow", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES, 256, false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> glowShader))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));
    }

    public static final Codec<StarGlowComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("color").forGetter(StarGlowComponent::color),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "radius", 3600.0).forGetter(StarGlowComponent::radius),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "opacity", 0.3).forGetter(StarGlowComponent::opacity),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "fadeDistance", 3000.0).forGetter(StarGlowComponent::fadeDistance),
                    MoreCodecs.strictOptionalField(Codec.INT, "skyColor", 0xFFFFFF).forGetter(StarGlowComponent::skyColor),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "skyOpacity", 0.5).forGetter(StarGlowComponent::skyOpacity)
            ).apply(instance, StarGlowComponent::new)
    );
}
