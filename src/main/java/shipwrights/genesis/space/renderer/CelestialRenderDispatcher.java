package shipwrights.genesis.space.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CelestialRenderDispatcher {

    private static Boolean oculusLoaded = null;

    private static boolean isOculusLoaded() {
        if (oculusLoaded == null) oculusLoaded = ModList.get().isLoaded("oculus");
        return oculusLoaded;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vec3 cameraPos = event.getCamera().getPosition();

        VantagePoint vantagePoint = VantagePoint.get(level, new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), ticks, partialTick);

        Vector3dc cameraForRenderOrder = vantagePoint instanceof VantagePoint.OnCelestial ? vantagePoint.getPosition() : VectorConversionsMCKt.toJOML(cameraPos);

        if (vantagePoint != null) {
            final Registry<Celestial> reg = registry;
            List<Celestial> celestials = registry.stream()
                .sorted(Comparator.comparingDouble(a -> -a.getPosition(ticks, partialTick, reg).distanceSquared(cameraForRenderOrder)))
                .toList();

            boolean shouldSkipCurrentPlanet = !GenesisClientConfig.shouldRenderCurrentPlanet();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            for (Celestial celestial : celestials) {
                if (shouldSkipCurrentPlanet && vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial() == celestial) {
                    continue;
                }

                CelestialType type = celestial.type();
                CelestialRenderer renderer = type.getRenderer();

                renderer.setup(event, vantagePoint);
                renderer.invoke(event, celestial, vantagePoint);
                renderer.teardown(event, vantagePoint);

                // Celestial shaders rely on per-object uniforms. Flush any
                // leftover batched geometry before the next celestial mutates
                // shared shader state.
                bufferSource.endBatch();
            }
        }
    }
}
