package shipwrights.genesis.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.client.components.AtmosphereComponent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/// Draws the {@link Skybox} and {@link Starfield}, then every celestial that has a {@link CelestialVisualizer}, furthest from the observer
/// first. The celestial the observer is standing on isn't drawn; the level itself is its surface.
///
/// Celestials are drawn just before the sky itself (see {@link shipwrights.genesis.mixin.LevelRendererMixin}), which is
/// then blended over them, except for what shines through the sky, which is drawn just after it.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public class CelestialRenderDispatcher {

    private static boolean renderedBehindSky = false;

    /// The depth between one celestial and the next nearest, when drawn around the sky
    private static final double DEPTH_STEP = 1e-5;

    /// Draws the stars and every celestial except what {@linkplain
    /// CelestialVisualizer.Component#shinesThroughSky() shines through the sky}. Called from the sky renderer, before
    /// the sky is drawn.
    ///
    /// Celestials are drawn at the nearest depths, so the sun, moon and stars drawn with the sky are hidden behind them.
    /// The sky itself is drawn without depth testing, so it still covers them. Each is a little nearer than those behind
    /// it, so what's drawn over the sky is still hidden by the celestials in front of it.
    ///
    /// They're drawn on black, so fading them out for the weather all at once only needs that darkened; the sky then
    /// blended over them shows through instead. Celestials have no other way to fade, being opaque.
    public static void renderBehindSky(PoseStack poseStack, Camera camera, float partialTick) {
        renderStars(poseStack, camera, partialTick);
        render(poseStack, camera, partialTick, component -> !component.shinesThroughSky(), 0, DEPTH_STEP);
        renderedBehindSky = true;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 cameraPos = camera.getPosition();
        VantagePoint vantagePoint = VantagePoint.get(level, Time.get(level, partialTick),
                new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z));
        float visibility = CelestialPlacement.weatherVisibility(level, vantagePoint, cameraPos.y, partialTick);
        if (visibility < 1f) {
            darkenScreen(1f - visibility);
        }
    }

    /// Scales everything drawn so far towards black by {@code amount}, from 0 for unchanged to 1 for black
    private static void darkenScreen(float amount) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        // Applying a shader applies its blend mode, unless that mode was the last one applied, and the position shader
        // asks for none. Apply it now, so applying it again to draw changes nothing and leaves our blending be.
        RenderSystem.getShader().apply();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(0, 0, 0, amount);

        // A quad covering the screen, drawn directly in clip space
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(-1, -1, 0).endVertex();
        builder.vertex(1, -1, 0).endVertex();
        builder.vertex(1, 1, 0).endVertex();
        builder.vertex(-1, 1, 0).endVertex();
        BufferUploader.drawWithShader(builder.end());

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        if (renderedBehindSky) {
            // At the same depths as behind the sky, so the celestials in front hide them
            render(event.getPoseStack(), event.getCamera(), event.getPartialTick(),
                    CelestialVisualizer.Component::shinesThroughSky, 0, DEPTH_STEP);

            // Nothing else has drawn depth yet, so this only puts back what the celestials around the sky changed
            RenderSystem.depthMask(true);
            RenderSystem.clearDepth(1);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } else {
            // The sky isn't drawn when fog, blindness etc. obscure it, or in dimensions without a normal sky. Drawn
            // furthest first, nearer celestials already cover the ones behind them.
            renderStars(event.getPoseStack(), event.getCamera(), event.getPartialTick());
            render(event.getPoseStack(), event.getCamera(), event.getPartialTick(), component -> true, 1, 0);
        }
        renderedBehindSky = false;
    }

    /// The skybox and stars behind every celestial. Hidden by daylight, and the skybox by the air too, showing as the
    /// camera climbs out of the atmosphere. In space, they're always fully bright.
    private static void renderStars(PoseStack poseStack, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 cameraPos = camera.getPosition();
        VantagePoint vantagePoint = VantagePoint.get(level, Time.get(level, partialTick),
                new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z));
        if (!vantagePoint.isReal()) return;

        float brightness = 1f;
        float skyboxBrightness = 1f;
        if (vantagePoint instanceof VantagePoint.OnCelestial oc) {
            brightness = 1f - CelestialPlacement.skyBrightness(level, cameraPos, partialTick);
            ResourceLocation id = oc.celestials().getKey(oc.celestial());
            float airDensity = id == null ? 0f : AtmosphereComponent.airDensity(id, cameraPos.y);
            skyboxBrightness = brightness * (1f - airDensity);
        }
        Skybox.render(poseStack, vantagePoint, skyboxBrightness);
        Starfield.render(poseStack, vantagePoint, brightness);
    }

    /// @param nearestDepth the depth the nearest celestial is drawn at; at 1 they're behind everything else in the level
    /// @param depthStep how much further each celestial is drawn than the next nearest
    private static void render(PoseStack poseStack, Camera camera, float partialTick,
                               Predicate<CelestialVisualizer.Component> filter, double nearestDepth, double depthStep) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        Registry<Celestial> celestials = level.registryAccess().registry(Celestial.REGISTRY_KEY).orElse(null);
        if (celestials == null) return;

        Time time = Time.get(level, partialTick);
        Vec3 cameraPos = camera.getPosition();
        Vector3dc cameraPosition = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);

        VantagePoint vantagePoint = VantagePoint.get(level, time, cameraPosition);
        if (!vantagePoint.isReal()) return;

        // In space the camera is already at its real position; on a celestial it's on that celestial's surface
        Vector3dc observer = vantagePoint instanceof VantagePoint.OnCelestial ? vantagePoint.getPosition() : cameraPosition;
        Celestial standingOn = vantagePoint instanceof VantagePoint.OnCelestial oc ? oc.celestial() : null;
        Registry<CelestialVisualizer> visualizers = CelestialVisualizer.REGISTRY.registry();

        var bufferSource = minecraft.renderBuffers().bufferSource();

        // Nearest first, so each celestial's depth doesn't depend on the filter
        List<Drawable> drawables = celestials.entrySet().stream()
                .filter(entry -> entry.getValue() != standingOn)
                .flatMap(entry -> visualizers.getOptional(entry.getKey().location()).stream().map(visualizer -> new Drawable(
                        entry.getValue(),
                        visualizer,
                        entry.getValue().transformProvider().getPosition(celestials, time).distanceSquared(observer)
                )))
                .sorted(Comparator.comparingDouble(Drawable::distanceSquared))
                .toList();

        // Celestials are far beyond the far plane. Depth clamping stops them being clipped by it, and as each is drawn
        // at a single fixed depth anyway, the depth they're clamped to doesn't matter.
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        // Render types only ever turn depth writes off, expecting them to be on, but the sky renderer turns them off
        RenderSystem.depthMask(true);
        for (int i = drawables.size() - 1; i >= 0; i--) {
            Drawable drawable = drawables.get(i);
            double depth = nearestDepth + i * depthStep;
            GL11.glDepthRange(depth, depth);

            drawable.visualizer().render(new CelestialRenderContext(
                    drawable.celestial(), vantagePoint, time, poseStack, camera, level
            ), filter);

            // Celestial shaders rely on per-object uniforms. Flush any leftover batched geometry before the next
            // celestial mutates shared shader state.
            bufferSource.endBatch();
        }
        GL11.glDepthRange(0, 1);
        GL11.glDisable(GL32.GL_DEPTH_CLAMP);
    }

    private record Drawable(Celestial celestial, CelestialVisualizer visualizer, double distanceSquared) {}
}
