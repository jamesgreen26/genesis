package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.space.client.DitheredVignette;

@Mixin(Gui.class)
public abstract class GuiMixin {

    /// Draws the vignette dithered, to hide its colour banding
    @WrapOperation(method = "renderVignette", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"))
    private void genesis$ditherVignette(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int blitOffset,
                                        float uOffset, float vOffset, int width, int height, int textureWidth,
                                        int textureHeight, Operation<Void> original) {
        if (!DitheredVignette.canDraw()) {
            original.call(guiGraphics, texture, x, y, blitOffset, uOffset, vOffset, width, height, textureWidth, textureHeight);
            return;
        }
        DitheredVignette.draw(guiGraphics.pose().last().pose(), texture, x, y, x + width, y + height, blitOffset,
                uOffset / textureWidth, vOffset / textureHeight,
                (uOffset + width) / textureWidth, (vOffset + height) / textureHeight);
    }
}
