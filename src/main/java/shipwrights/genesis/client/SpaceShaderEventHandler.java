package shipwrights.genesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisClientConfig;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GenesisMod.MOD_ID)
public class SpaceShaderEventHandler {

    private static Boolean oculusLoaded = null;

    private static boolean isOculusLoaded() {
        if (oculusLoaded == null) oculusLoaded = ModList.get().isLoaded("oculus");
        return oculusLoaded;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Update shader state at the start of each frame
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level != null) {
            boolean shouldBeActive = GenesisMod.isSpaceDimension(level) && GenesisClientConfig.enableSpaceLighting() && !isOculusLoaded();
            SpaceInvertPostProcessor.INSTANCE.setActive(shouldBeActive);
        }
    }
}
