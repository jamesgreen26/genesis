package shipwrights.genesis.space.client.map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/// A button showing an item in place of its text, like a {@link net.minecraft.client.gui.components.SpriteIconButton}
/// but for an item's icon. Its message is still used for its tooltip and narration.
public class ItemIconButton extends Button {
    private final ItemStack icon;

    public ItemIconButton(int x, int y, int size, ItemStack icon, Component message, OnPress onPress) {
        super(x, y, size, size, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(Tooltip.create(message));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.renderItem(icon, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
    }
}
