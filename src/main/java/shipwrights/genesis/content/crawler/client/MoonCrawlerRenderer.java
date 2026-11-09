package shipwrights.genesis.content.crawler.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.GenesisEntityTypes;
import shipwrights.genesis.content.crawler.MoonCrawler;

/// Mirrors vanilla's cave spider renderer, with the moon crawler's texture
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MoonCrawlerRenderer extends SpiderRenderer<MoonCrawler> {
    private static final ResourceLocation TEXTURE = GenesisMod.id("textures/entity/moon_crawler.png");
    private static final float SCALE = 0.7F;

    public MoonCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.CAVE_SPIDER);
        this.shadowRadius *= SCALE;
    }

    @Override
    protected void scale(MoonCrawler entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonCrawler entity) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GenesisEntityTypes.MOON_CRAWLER.get(), MoonCrawlerRenderer::new);
    }
}
