package shipwrights.genesis.space.client.map.editor;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import shipwrights.genesis.util.MoreCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

/// What could be typed in the focused {@link ArgumentBox}, listed under it as a command's arguments are in chat: Tab
/// takes the one picked, the first unless another's been picked with the arrow keys, which Enter takes too, and Escape
/// hides them until something else is typed. With nothing to suggest, why what's typed won't do, if it won't, or what
/// to look out for, if it will, or failing those what may be typed, such as a number's bounds.
///
/// For a colour, a {@link ColorPicker} to pick it with instead, unless a dye's name is being typed.
///
/// Drawn over everything else, so has to be handed the screen's input before its widgets are.
final class SuggestionPopup {
    private static final int MAX_SHOWN = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 3;
    private static final int SWATCH = 8;
    private static final int BACKGROUND = 0xF0101018;
    private static final int BORDER = 0xFF505064;
    private static final int SELECTED = 0xFF303048;
    private static final int TEXT = 0xE0E0E0;
    private static final int SELECTED_TEXT = 0xFFFF60;
    private static final int TOOLTIP = 0x909090;
    private static final int INFO = 0xA0A0B0;

    private final Font font;
    /// Where a box has to be for its suggestions to be shown, as the form scrolls them out of sight
    private final Supplier<ScreenRectangle> area;
    private final ColorPicker picker = new ColorPicker();

    @Nullable
    private ArgumentBox box;
    private List<Suggestion> suggestions = List.of();
    /// Which suggestion's picked, and whether that was with the arrow keys, rather than by default
    private int selected;
    private boolean chosen;
    /// How far down the list it's scrolled
    private int offset;
    /// Hidden with Escape, until something else is typed
    private boolean dismissed;
    private String lastText = "";
    /// Counts the times suggestions are asked for, so only the last answer's taken
    private int generation;

    SuggestionPopup(Font font, Supplier<ScreenRectangle> area) {
        this.font = font;
        this.area = area;
    }

    /// Stops suggesting, as when the boxes are made afresh
    void clear() {
        picker.release();
        if (box != null) box.setSuggestion(null);
        box = null;
        suggestions = List.of();
    }

    void focusChanged(ArgumentBox box, boolean focused) {
        if (focused) {
            this.box = box;
            dismissed = false;
            lastText = box.getValue();
            update(box);
        } else if (this.box == box) {
            clear();
        }
    }

    /// Asks for suggestions again, as what's typed in {@code box} or where its cursor is has changed
    void update(ArgumentBox box) {
        if (box != this.box) return;
        String text = box.getValue();
        if (!text.equals(lastText)) {
            lastText = text;
            dismissed = false;
        }
        int cursor = Math.min(box.getCursorPosition(), text.length());
        int asked = ++generation;
        box.argument().suggest(text.substring(0, cursor)).thenAccept(answer -> {
            if (asked == generation && this.box == box) show(box, answer);
        });
    }

    private void show(ArgumentBox box, Suggestions answer) {
        String text = box.getValue();
        List<Suggestion> shown = new ArrayList<>();
        for (Suggestion suggestion : answer.getList()) {
            // Not those that'd change nothing
            if (!suggestion.apply(text).equals(text)) shown.add(suggestion);
        }
        suggestions = shown;
        selected = 0;
        chosen = false;
        offset = 0;
        updateGhost();
    }

    /// The rest of the suggestion picked, shown faintly after what's typed, when it carries on from it
    private void updateGhost() {
        if (box == null) return;
        String text = box.getValue();
        String ghost = null;
        if (isShowingSuggestions() && box.getCursorPosition() == text.length()) {
            String applied = suggestions.get(selected).apply(text);
            if (applied.startsWith(text)) ghost = applied.substring(text.length());
        }
        box.setSuggestion(ghost);
    }

    private boolean isShowing() {
        if (box == null || !box.isFocused() || !box.isVisible() || dismissed) return false;
        ScreenRectangle area = this.area.get();
        return box.getY() >= area.top() && box.getBottom() <= area.bottom();
    }

    private boolean isShowingSuggestions() {
        return isShowing() && !suggestions.isEmpty();
    }

    /// Whether it's showing the colour picker: for a colour, while no dye's name is being suggested
    private boolean isPicking() {
        return isShowing() && suggestions.isEmpty() && box.argument().color("#000000").isPresent();
    }

    /// Puts the colour picked in the box, as what's typed there
    private void applyPicked() {
        String hex = MoreCodecs.hexColor(picker.rgb());
        if (!hex.equalsIgnoreCase(box.getValue().trim())) box.replace(hex);
    }

    /// Why what's typed won't do, or failing that what to look out for in it, or what may be typed
    @Nullable
    private Component problem() {
        if (box == null) return null;
        if (!box.getValue().isEmpty()) {
            if (box.error() != null) return box.error();
            if (box.warning() != null) return box.warning();
        }
        return box.info().orElse(null);
    }

    private int problemColor() {
        if (box.getValue().isEmpty()) return INFO;
        return box.error() != null ? ArgumentBox.INVALID_COLOR : box.warning() != null ? ArgumentBox.WARNING_COLOR : INFO;
    }

    private void take(int index) {
        if (box == null || index < 0 || index >= suggestions.size()) return;
        Suggestion suggestion = suggestions.get(index);
        ArgumentBox taken = box;
        taken.setValue(suggestion.apply(taken.getValue()));
        taken.setCursorPosition(suggestion.getRange().getStart() + suggestion.getText().length());
        taken.setHighlightPos(taken.getCursorPosition());
        // Once taken, there's nothing more to suggest until more's typed
        dismissed = true;
        lastText = taken.getValue();
        taken.setSuggestion(null);
    }

    private void select(int index) {
        selected = Mth.positiveModulo(index, suggestions.size());
        chosen = true;
        offset = Mth.clamp(offset, selected - MAX_SHOWN + 1, selected);
        updateGhost();
    }

    // Input

    boolean keyPressed(int keyCode) {
        if (!isShowing()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (!suggestions.isEmpty() || problem() != null)) {
            dismissed = true;
            box.setSuggestion(null);
            return true;
        }
        if (suggestions.isEmpty()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> select(selected - 1);
            case GLFW.GLFW_KEY_DOWN -> select(selected + 1);
            case GLFW.GLFW_KEY_TAB -> take(selected);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                // Only once one's been picked: Enter otherwise means what's typed is done with
                if (!chosen) return false;
                take(selected);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        if (isPicking()) {
            Layout layout = layout();
            if (!layout.contains(mouseX, mouseY)) return false;
            if (picker.press(mouseX, mouseY, layout.left + PADDING, layout.top + PADDING)) applyPicked();
            // Anywhere on it, so the box under it isn't clicked through it
            return true;
        }
        if (!isShowingSuggestions()) return false;
        Layout layout = layout();
        if (!layout.contains(mouseX, mouseY)) return false;
        int index = offset + (int) ((mouseY - layout.top - PADDING) / LINE_HEIGHT);
        if (index >= offset && index < Math.min(suggestions.size(), offset + MAX_SHOWN)) take(index);
        return true;
    }

    /// Carries on picking a colour, once pressed on the picker
    boolean mouseDragged(double mouseX, double mouseY) {
        if (!picker.isDragging()) return false;
        if (!isPicking()) {
            picker.release();
            return false;
        }
        Layout layout = layout();
        picker.drag(mouseX, mouseY, layout.left + PADDING, layout.top + PADDING);
        applyPicked();
        return true;
    }

    boolean mouseReleased() {
        if (!picker.isDragging()) return false;
        picker.release();
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isShowingSuggestions() || !layout().contains(mouseX, mouseY)) return false;
        offset = Mth.clamp(offset - (int) Math.signum(scrollY), 0, Math.max(0, suggestions.size() - MAX_SHOWN));
        return true;
    }

    // Drawing

    private record Layout(int left, int top, int width, int height) {
        boolean contains(double x, double y) {
            return x >= left && x < left + width && y >= top && y < top + height;
        }
    }

    /// Under the box, or over it if there's no room under it, and on screen
    private Layout layout() {
        int lines;
        int width;
        int extra = 0;
        if (isShowingSuggestions()) {
            lines = Math.min(MAX_SHOWN, suggestions.size());
            width = 0;
            for (Suggestion suggestion : suggestions) width = Math.max(width, lineWidth(suggestion));
        } else if (isPicking()) {
            // The picker, then what there is to know about the colour, if anything, under it
            Component problem = problem();
            lines = problem == null ? 0 : font.split(problem, 200).size();
            width = Math.max(ColorPicker.WIDTH, problem == null ? 0 : Math.min(200, font.width(problem)));
            extra = ColorPicker.HEIGHT + (lines > 0 ? PADDING + 2 : 2);
        } else {
            Component problem = problem();
            lines = problem == null ? 0 : font.split(problem, 200).size();
            width = problem == null ? 0 : Math.min(200, font.width(problem));
        }
        width += PADDING * 2;
        int height = lines * LINE_HEIGHT + PADDING * 2 - 2 + extra;
        Window window = Minecraft.getInstance().getWindow();
        int left = Math.min(box.getX(), window.getGuiScaledWidth() - width);
        int top = box.getBottom() + 1;
        if (top + height > window.getGuiScaledHeight() && box.getY() - height - 1 >= 0) top = box.getY() - height - 1;
        return new Layout(Math.max(0, left), top, width, height);
    }

    private int lineWidth(Suggestion suggestion) {
        int width = font.width(suggestion.getText());
        if (swatch(suggestion).isPresent()) width += SWATCH + 3;
        if (suggestion.getTooltip() != null) width += 8 + font.width(ComponentUtils.fromMessage(suggestion.getTooltip()));
        return width;
    }

    private OptionalInt swatch(Suggestion suggestion) {
        return box == null ? OptionalInt.empty() : box.argument().color(suggestion.getText());
    }

    void render(GuiGraphics guiGraphics) {
        if (!isShowing()) return;
        boolean showingSuggestions = isShowingSuggestions();
        boolean picking = isPicking();
        if (!showingSuggestions && !picking && problem() == null) return;

        Layout layout = layout();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        guiGraphics.fill(layout.left, layout.top, layout.left + layout.width, layout.top + layout.height, BACKGROUND);
        guiGraphics.renderOutline(layout.left, layout.top, layout.width, layout.height, BORDER);

        int x = layout.left + PADDING;
        int y = layout.top + PADDING;
        if (showingSuggestions) {
            for (int i = offset; i < Math.min(suggestions.size(), offset + MAX_SHOWN); i++) {
                Suggestion suggestion = suggestions.get(i);
                if (i == selected) {
                    guiGraphics.fill(layout.left + 1, y - 1, layout.left + layout.width - 1, y + LINE_HEIGHT - 1, SELECTED);
                }
                int textX = x;
                OptionalInt color = swatch(suggestion);
                if (color.isPresent()) {
                    guiGraphics.fill(textX, y, textX + SWATCH, y + SWATCH, 0xFF000000 | color.getAsInt());
                    textX += SWATCH + 3;
                }
                textX = guiGraphics.drawString(font, suggestion.getText(), textX, y + 1, i == selected ? SELECTED_TEXT : TEXT);
                if (suggestion.getTooltip() != null) {
                    guiGraphics.drawString(font, ComponentUtils.fromMessage(suggestion.getTooltip()), textX + 8, y + 1, TOOLTIP);
                }
                y += LINE_HEIGHT;
            }
            // More above or below
            if (offset > 0) guiGraphics.fill(layout.left + layout.width - 3, layout.top + 1, layout.left + layout.width - 1, layout.top + 4, TOOLTIP | 0xFF000000);
            if (offset + MAX_SHOWN < suggestions.size()) {
                int bottom = layout.top + layout.height;
                guiGraphics.fill(layout.left + layout.width - 3, bottom - 4, layout.left + layout.width - 1, bottom - 1, TOOLTIP | 0xFF000000);
            }
        } else {
            if (picking) {
                // Following what's typed, unless it's what's being picked
                OptionalInt typed = box.argument().color(box.getValue());
                if (typed.isPresent() && !picker.isDragging()) picker.sync(typed.getAsInt());
                picker.render(guiGraphics, x, y);
                y += ColorPicker.HEIGHT + PADDING + 2;
                if (problem() == null) {
                    guiGraphics.pose().popPose();
                    return;
                }
            }
            int color = problemColor();
            for (var line : font.split(problem(), 200)) {
                guiGraphics.drawString(font, line, x, y + 1, color);
                y += LINE_HEIGHT;
            }
        }
        guiGraphics.pose().popPose();
    }
}
