package shipwrights.dataplanets.entities;

import shipwrights.dataplanets.registry.DPItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class NeumEntityRenderer extends EntityRenderer<NeumEntity> {
    public NeumEntityRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
    }



    @Override
    public ResourceLocation getTextureLocation(NeumEntity neumEntity) {
        return ResourceLocation.fromNamespaceAndPath("dataplanets","neum");
    }

    @Override
    public void render(NeumEntity neum, float p_114486_, float p_114487_, PoseStack pose, MultiBufferSource p_114489_, int p_114490_) {
        pose.translate(0,0.5f,0);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.scale(0.8f,0.8f,0.8f);
        pose.translate(0.1f,0.1f,0f);
        ir.renderStatic(new ItemStack(Blocks.IRON_BLOCK), ItemDisplayContext.NONE, p_114490_, OverlayTexture.NO_OVERLAY,pose,p_114489_,neum.level(),0);
        pose.popPose();

        pose.pushPose();
        ir.renderStatic(new ItemStack(DPItems.NEUM_TRACKS), ItemDisplayContext.NONE, p_114490_, OverlayTexture.NO_OVERLAY,pose,p_114489_,neum.level(),0);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.YP.rotation(neum.yHeadRot));
        pose.translate(0,1,0);
        ir.renderStatic(new ItemStack(DPItems.NEUM_HEAD), ItemDisplayContext.NONE, p_114490_, OverlayTexture.NO_OVERLAY,pose,p_114489_,neum.level(),0);

        pose.popPose();
    }
}
