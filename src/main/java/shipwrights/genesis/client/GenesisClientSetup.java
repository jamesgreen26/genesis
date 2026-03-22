package shipwrights.genesis.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.blockentityRenderer.NavProjectorBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.RadarDisplayBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidCoreBlockEntityRenderer;
import shipwrights.genesis.client.blockentityRenderer.VoidEngineInterfaceBlockEntityRenderer;
import shipwrights.genesis.content.block.GenesisBlocks;
import shipwrights.genesis.content.blockentity.GenesisBlockEntities;
import shipwrights.genesis.content.fluid.GenesisFluids;
import shipwrights.genesis.content.particle.GenesisParticles;
import shipwrights.genesis.content.particle.VerditeParticle;
import shipwrights.genesis.content.particle.ZapBubbleParticle;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GenesisClientSetup {
    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(GenesisBlockEntities.NAV_PROJECTOR.get(), NavProjectorBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.RADAR_DISPLAY.get(), RadarDisplayBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_CORE.get(), VoidCoreBlockEntityRenderer::new);
            BlockEntityRenderers.register(GenesisBlockEntities.VOID_ENGINE_INTERFACE.get(), VoidEngineInterfaceBlockEntityRenderer::new);
            MenuScreens.register(GenesisBlocks.TULCITE_CATALYZER_CONTAINER.get(), TulciteCatalyzerScreen::new);

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
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.SPINDLE_BUSH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.SPINDLE_GRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.TALL_SPINDLE_GRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.BELL_FLOWER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.FLOWERING_BEARD_SHROOM_LEAVES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.BEARD_SHROOM_LEAVES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.HANGING_BEARD_SHROOM_LEAVES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.HANGING_BEARD_SHROOM_LEAVES_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.ZAPLIGHT_SPINDLES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.WALL_ZAPLIGHT_SPINDLES.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.VERDITE_CLUSTER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.MEDIUM_VERDITE_BUD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.LARGE_VERDITE_BUD.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.SMALL_VERDITE_BUD.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.MALACHITE_BRAMBLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.MALACHITE_SPROUTS.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.HEMATITE_BRAMBLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.HEMATITE_SPROUTS.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.CHALCOPYRITE_BRAMBLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.CHALCOPYRITE_SPROUTS.get(), RenderType.cutout());

            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.WITHERING_WILLOW_BRANCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GenesisBlocks.WITHERING_WILLOW_LEAVES.get(), RenderType.cutout());

            // Miasma fluid render type (must be registered client-side to avoid server crash)
            ItemBlockRenderTypes.setRenderLayer(GenesisFluids.MIASMA.getSource(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(GenesisFluids.MIASMA.get(), RenderType.translucent());
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
                ShaderRegistry.clearTexturedPlanetRenderTypes();
                GenesisMod.LOGGER.debug("Cleared planet texture caches");
            }
        });
    }
    @SubscribeEvent
    public static void registerParticleProvider(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(GenesisParticles.ZAP_BUBBLE_PARTICLES.get(), ZapBubbleParticle.Provider::new);
        event.registerSpriteSet(GenesisParticles.VERDITE_PARTICLES.get(), VerditeParticle.Provider::new);
    }
}
