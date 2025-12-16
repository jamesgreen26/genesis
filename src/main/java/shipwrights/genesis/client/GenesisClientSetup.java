package shipwrights.genesis.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import shipwrights.genesis.GenesisBlocks;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.blockentity.GenesisBlockEntities;
import shipwrights.genesis.client.blockentityRenderer.NavProjectorBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.RadarDisplayBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidCoreBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidEngineInterfaceBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

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

            // IDK why it's whining, it says that it's depreciated for 1.21.4+, but were not on that version sooo
            // also this is how im changing render types for plants
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.DEAD_MOON_CORAL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.DEAD_MOON_CORAL_FAN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.DEAD_MOON_CORAL_WALL_FAN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.BRINE_FLOWER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.BRINE_TRUNK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.PETRIFIED_BUSH.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // Register a reload listener to clear planet texture caches when resources are reloaded
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(@NotNull Void object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                PlanetTextures.clearCache();
                ShaderRegistry.clearTexturedPlanetRenderTypes();
                GenesisMod.LOGGER.debug("Cleared planet texture caches");
            }
        });
    }
}
