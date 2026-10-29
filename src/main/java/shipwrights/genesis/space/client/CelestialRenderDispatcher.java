package shipwrights.genesis.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL11;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/// Draws every celestial that has a {@link CelestialVisualizer}, furthest from the observer first.
///
/// Celestials in the sky are drawn just before the sky itself (see
/// {@link shipwrights.genesis.mixin.LevelRendererMixin}), which is then blended over them, except for what shines
/// through the sky, which is drawn just after it. The celestial the observer is standing on is drawn after the sky, so
/// the sky doesn't tint the ground below.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public class CelestialRenderDispatcher {

    private static boolean renderedBehindSky = false;

    /// The depth between one celestial and the next nearest, when drawn around the sky
    private static final double DEPTH_STEP = 1e-5;

    /// Draws every celestial except the one underfoot, and except what {@linkplain
    /// CelestialVisualizer.Component#shinesThroughSky() shines through the sky}. Called from the sky renderer, before
    /// the sky is drawn.
    ///
    /// Celestials are drawn at the nearest depths, so the sun, moon and stars drawn with the sky are hidden behind them.
    /// The sky itself is drawn without depth testing, so it still covers them. Each is a little nearer than those behind
    /// it, so what's drawn over the sky is still hidden by the celestials in front of it.
    public static void renderBehindSky(PoseStack poseStack, Camera camera, float partialTick) {
        render(poseStack, camera, partialTick, false, component -> !component.shinesThroughSky(), 0, DEPTH_STEP);
        renderedBehindSky = true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        if (renderedBehindSky) {
            // At the same depths as behind the sky, so the celestials in front hide them
            render(event.getPoseStack(), event.getCamera(), event.getPartialTick(), false,
                    CelestialVisualizer.Component::shinesThroughSky, 0, DEPTH_STEP);

            // Nothing else has drawn depth yet, so this only puts back what the celestials around the sky changed
            RenderSystem.depthMask(true);
            RenderSystem.clearDepth(1);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } else {
            // The sky isn't drawn when fog, blindness etc. obscure it, or in dimensions without a normal sky. Drawn
            // furthest first, nearer celestials already cover the ones behind them.
            render(event.getPoseStack(), event.getCamera(), event.getPartialTick(), false, component -> true, 1, 0);
        }
        renderedBehindSky = false;

        render(event.getPoseStack(), event.getCamera(), event.getPartialTick(), true, component -> true, 1, 0);
    }

    /// @param nearestDepth the depth the nearest celestial is drawn at; at 1 they're behind everything else in the level
    /// @param depthStep how much further each celestial is drawn than the next nearest
    private static void render(PoseStack poseStack, Camera camera, float partialTick, boolean underfoot,
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
                .filter(entry -> (entry.getValue() == standingOn) == underfoot)
                .flatMap(entry -> visualizers.getOptional(entry.getKey().location()).stream().map(visualizer -> new Drawable(
                        entry.getValue(),
                        visualizer,
                        entry.getValue().transformProvider().getPosition(celestials, time).distanceSquared(observer)
                )))
                .sorted(Comparator.comparingDouble(Drawable::distanceSquared))
                .toList();

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
    }

    private record Drawable(Celestial celestial, CelestialVisualizer visualizer, double distanceSquared) {}
}
