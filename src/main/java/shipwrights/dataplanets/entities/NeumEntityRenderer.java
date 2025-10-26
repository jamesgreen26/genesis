package shipwrights.dataplanets.entities;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NeumEntityRenderer extends MobRenderer<NeumEntity,NeumModel> {


    public NeumEntityRenderer(EntityRendererProvider.Context arg) {
        super(arg, new NeumModel(), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NeumEntity neumEntity) {
        return ResourceLocation.fromNamespaceAndPath("beings","neum");
    }

}
