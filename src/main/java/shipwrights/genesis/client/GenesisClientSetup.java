package shipwrights.genesis.client;

import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.blockentity.GenesisBlockEntities;
import shipwrights.genesis.client.renderer.NavProjectorBlockEntityRenderer;
import shipwrights.genesis.client.renderer.VoidCoreBlockEntityRenderer;
import shipwrights.genesis.client.renderer.VoidEngineInterfaceBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GenesisClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(GenesisBlockEntities.NAV_PROJECTOR.get(), NavProjectorBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_CORE.get(), VoidCoreBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_ENGINE_INTERFACE.get(), VoidEngineInterfaceBlockEntityRenderer::new);
        });
    }
}
