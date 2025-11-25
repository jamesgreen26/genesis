package shipwrights.genesis.client;

import net.minecraft.client.gui.screens.MenuScreens;
import shipwrights.genesis.GenesisBlocks;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.blockentity.GenesisBlockEntities;
import shipwrights.genesis.client.blockentityRenderer.NavProjectorBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.RadarDisplayBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidCoreBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidEngineInterfaceBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;

@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GenesisClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(GenesisBlockEntities.NAV_PROJECTOR.get(), NavProjectorBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.RADAR_DISPLAY.get(), RadarDisplayBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_CORE.get(), VoidCoreBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_ENGINE_INTERFACE.get(), VoidEngineInterfaceBlockEntityRenderer::new);
            MenuScreens.register(GenesisBlocks.WARPSTONE_CATALYZER_CONTAINER.get(), WarpstoneCatalyzerScreen::new);

            // Register post-processing shader for space dimension
            PostProcessHandler.addInstance(SpaceInvertPostProcessor.INSTANCE);
        });
    }
}
