package shipwrights.dataplanets.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import shipwrights.dataplanets.registry.DPItems;

public class NeumModel extends EntityModel<NeumEntity> {
    NeumEntity neum;
    @Override
    public void setupAnim(NeumEntity arg, float f, float g, float h, float i, float j) {
        this.neum=arg;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer arg2, int i, int j, float f, float g, float h, float k) {

        pose.translate(0,1,0);
        //pose.mulPose(neum.getMotionDirection().getRotation());
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        MultiBufferSource bufferSource  = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        pose.scale(0.8f,0.8f,0.8f);
        pose.translate(0.1f,0,0f);
        ir.renderStatic(new ItemStack(BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(neum.block))), ItemDisplayContext.NONE, i,j,pose,bufferSource,neum.level(),0);
        pose.popPose();


        pose.pushPose();
        ir.renderStatic(new ItemStack(DPItems.NEUM_TRACKS.get()), ItemDisplayContext.NONE,i, j,pose,bufferSource,neum.level(),0);
        pose.popPose();

        pose.pushPose();
        pose.translate(0,0.9f,0);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        ir.renderStatic(new ItemStack(DPItems.NEUM_HEAD.get()), ItemDisplayContext.NONE,i, j,pose,bufferSource,neum.level(),0);


        pose.popPose();
    }
}
