package shipwrights.genesis.space.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

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

        Celestial vantagePoint = GenesisMod.getCelestialForLevel(level);

        if (vantagePoint != null || GenesisMod.isSpaceDimension(level)) {
            for (Celestial celestial : GenesisMod.SPACE_REGISTRY.getAll()) {
                celestial.getType().getRenderer().invoke(event, celestial, vantagePoint);
            }
        }
    }
}
