package shipwrights.genesis.space.client.map.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// A scrolling list of labelled rows of widgets, as the editor's forms are laid out. Its rows come from a supplier, and
/// are made afresh from it when {@linkplain #requestRebuild asked}, such as when the form's shape changes.
final class FormList extends ContainerObjectSelectionList<FormList.Row> {
    static final int ROW_HEIGHT = 20;
    /// How far each level of nesting is indented
    private static final int INDENT = 8;

    private Supplier<List<Row>> rows = List::of;
    private boolean needsRebuild;

    FormList(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_HEIGHT);
    }

    /// Shows the rows {@code rows} gives, now and whenever {@linkplain #requestRebuild rebuilt}
    void show(Supplier<List<Row>> rows) {
        this.rows = rows;
        setScrollAmount(0);
        rebuild();
    }

    /// Makes the rows afresh before the list's next drawn, keeping where it's scrolled to. Not straight away, as this
    /// is usually asked for by one of the rows' own widgets, while it's handling input.
    void requestRebuild() {
        needsRebuild = true;
    }

    private void rebuild() {
        needsRebuild = false;
        double scroll = getScrollAmount();

        // What's being typed in, to carry on in the same place in the new rows
        String focusedKey = null;
        int focusedIndex = -1;
        int cursor = 0;
        if (getFocused() instanceof Row row && row.key != null && row.getFocused() instanceof AbstractWidget widget) {
            focusedKey = row.key;
            focusedIndex = row.children().indexOf(widget);
            if (widget instanceof EditBox box) cursor = box.getCursorPosition();
            // Told it's lost focus, as nothing else tells it once it's gone
            widget.setFocused(false);
        }

        replaceEntries(rows.get());
        setScrollAmount(scroll);

        if (focusedKey == null) return;
        for (Row row : children()) {
            if (!focusedKey.equals(row.key) || focusedIndex >= row.children().size()) continue;
            GuiEventListener widget = row.children().get(focusedIndex);
            setFocused(row);
            row.setFocused(widget);
            widget.setFocused(true);
            if (widget instanceof EditBox box) box.moveCursorTo(cursor, false);
            break;
        }
    }

    /// The row at a point on screen, if any
    @Nullable
    Row rowAt(double x, double y) {
        return isMouseOver(x, y) ? getEntryAtPosition(x, y) : null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (needsRebuild) rebuild();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public int getRowWidth() {
        return getWidth() - 14;
    }

    @Override
    protected int getScrollbarPosition() {
        return getRight() - 6;
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    /// Something drawn in a row besides its label and widgets, such as a picture
    interface Painter {
        /// @param left where the row's label starts
        void paint(GuiGraphics guiGraphics, Font font, int left, int top, int width, int height);
    }

    /// A label, indented by its depth, with widgets along the right
    static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Font font;
        private final Component label;
        private final int depth;
        @Nullable
        private final AbstractWidget leading;
        private final List<AbstractWidget> widgets;
        private final int labelColor;
        @Nullable
        private Painter painter;
        private int labelOffset;
        /// Where in the form it is, so the same row's found once the form's built again
        @Nullable
        private String key;

        /// @param leading a widget before the label, such as a checkbox
        Row(Font font, Component label, int depth, @Nullable AbstractWidget leading, List<AbstractWidget> widgets,
            int labelColor) {
            this.font = font;
            this.label = label;
            this.depth = depth;
            this.leading = leading;
            this.widgets = List.copyOf(widgets);
            this.labelColor = labelColor;
        }

        Row(Font font, Component label, int depth, @Nullable AbstractWidget leading, List<AbstractWidget> widgets) {
            this(font, label, depth, leading, widgets, 0xFFFFFF);
        }

        Row keyed(String key) {
            this.key = key;
            return this;
        }

        Row painted(Painter painter, int labelOffset) {
            this.painter = painter;
            this.labelOffset = labelOffset;
            return this;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX,
                           int mouseY, boolean hovering, float partialTick) {
            int x = left + depth * INDENT;
            if (leading != null) {
                leading.setPosition(x, top + (height - leading.getHeight()) / 2);
                leading.render(guiGraphics, mouseX, mouseY, partialTick);
                x += leading.getWidth() + 2;
            }
            if (painter != null) painter.paint(guiGraphics, font, x, top, width, height);
            x += labelOffset;

            int right = left + width;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget widget = widgets.get(i);
                right -= widget.getWidth();
                widget.setPosition(right, top + (height - widget.getHeight()) / 2);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
                right -= 2;
            }

            int space = right - x - 2;
            if (space > 0) {
                FormattedCharSequence text = font.split(label, Integer.MAX_VALUE).isEmpty() ? FormattedCharSequence.EMPTY
                        : font.width(label) <= space ? label.getVisualOrderText()
                        : FormattedCharSequence.composite(font.split(label, Math.max(1, space - font.width("…"))).get(0),
                        Component.literal("…").getVisualOrderText());
                guiGraphics.drawString(font, text, x, top + (height - font.lineHeight) / 2 + 1, labelColor);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            List<AbstractWidget> all = new ArrayList<>();
            if (leading != null) all.add(leading);
            all.addAll(widgets);
            return all;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            List<AbstractWidget> all = new ArrayList<>();
            if (leading != null) all.add(leading);
            all.addAll(widgets);
            return all;
        }
    }
}
