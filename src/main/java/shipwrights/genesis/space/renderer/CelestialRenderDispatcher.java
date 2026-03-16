package shipwrights.genesis.space.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CelestialRenderDispatcher {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vec3 cameraPos = event.getCamera().getPosition();

        VantagePoint vantagePoint = VantagePoint.get(level, new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), ticks, partialTick);

        Vector3dc cameraForRenderOrder = vantagePoint instanceof VantagePoint.OnCelestial ? vantagePoint.getPosition() : VectorConversionsMCKt.toJOML(cameraPos);

        if (vantagePoint != null) {
            List<Celestial> celestials = GenesisMod.SPACE_REGISTRY.getAll().stream()
                .sorted(Comparator.comparingDouble(a -> -a.getPosition(ticks, partialTick).distanceSquared(cameraForRenderOrder)))
                .toList();

            for (Celestial celestial : celestials) {
                CelestialType type = celestial.getType();
                type.getRenderer().setup(event, vantagePoint);
                type.getRenderer().invoke(event, celestial, vantagePoint);
                type.getRenderer().teardown(event, vantagePoint);
            }
        }
    }
}
