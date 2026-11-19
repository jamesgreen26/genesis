package shipwrights.genesis.space.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;

/// What Escape opens in the {@link SolarSystemScreen}, as the pause menu does in a world: back to the editor, export
/// the project, or save and quit to the project list. Drawn over the map, dimmed.
final class EditorMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 204;

    private final SolarSystemScreen map;
    private Button exportButton;

    EditorMenuScreen(SolarSystemScreen map) {
        super(Component.literal(map.project().name()));
        this.map = map;
    }

    @Override
    protected void init() {
        // As the map would be laid out now, if the window's changed size while it's been away
        if (map.width != width || map.height != height) map.resize(minecraft, width, height);

        int x = width / 2 - BUTTON_WIDTH / 2;
        int y = height / 4 + 32;
        addRenderableWidget(Button.builder(Component.translatable("genesis.editor.menu.back"), button -> onClose())
                .bounds(x, y, BUTTON_WIDTH, 20).build());
        exportButton = addRenderableWidget(Button.builder(Component.translatable("genesis.editor.export"), button -> {
                    map.panel().export();
                    updateExportButton();
                })
                .bounds(x, y + 24, BUTTON_WIDTH, 20).build());
        updateExportButton();
        addRenderableWidget(Button.builder(Component.translatable("genesis.editor.menu.quit"), button -> map.onClose())
                .bounds(x, y + 48, BUTTON_WIDTH, 20).build());
    }

    /// Only active when everything's ready; otherwise its tooltip says what isn't
    private void updateExportButton() {
        exportButton.active = map.panel().isExportable();
        exportButton.setTooltip(Tooltip.create(map.panel().exportTooltip()));
    }

    /// Back to the editor
    @Override
    public void onClose() {
        minecraft.setScreen(map);
    }

    @Override
    public void tick() {
        // Saving what was left unsaved, and so on, as if the map were still open
        map.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 4 + 8, 0xFFFFFF);
    }

    /// The map and editor, as they were left, dimmed. Drawn with the mouse nowhere, so nothing on them looks hovered.
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        map.render(guiGraphics, -1, -1, partialTick);
        guiGraphics.flush();
        // Over all of it, whatever depths its labels and widgets were drawn at
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        renderTransparentBackground(guiGraphics);
    }
}
