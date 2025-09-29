package shipwrights.dataplanets.screens;

import shipwrights.dataplanets.blocks.ResearchStationBE;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;

public class ResearchScreen extends Screen {
    CompoundTag update;
    public ResearchScreen(ResearchStationBE researchStationBE) {
        super(Component.empty().append("Research"));
        update=researchStationBE.getUpdateTag();
    }

    @Override
    protected void init() {
        super.init();
        //this.addRenderableWidget(new Button.Builder(Component.empty().append(""),a->{}).bounds().build());
    }

    @Override
    public void render(GuiGraphics context, int p_281550_, int p_282878_, float p_282465_) {
        super.render(context, p_281550_, p_282878_, p_282465_);
        renderBackground(context);
        ListTag datapoints = (ListTag) update.get("datapoints");
        context.drawString(font,"Research",2 + width / 4,25, Color.WHITE.getRGB(),true);
        context.drawString(font,"Progress: "+update.getInt("progress"),2 + width / 4,100, Color.WHITE.getRGB(),true);
        if(datapoints!=null)
        {
            context.drawString(font,"Efficiency: "+datapoints.size(),2 + width / 4,120, Color.WHITE.getRGB(),true);
        }
    }

    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild("dataplanets","textures/gui/research_bg.png");
    @Override
    public void renderBackground(GuiGraphics context) {
        super.renderBackground(context);
        int x = width / 4;
        int y = height / 7;
        context.blit(TEXTURE,x,y,0,0,247,167);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
