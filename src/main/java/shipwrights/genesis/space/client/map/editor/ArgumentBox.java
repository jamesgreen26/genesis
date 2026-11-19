package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/// A text box for one value, read by its {@link FieldArgument} as it's typed: each time it reads, the value's handed on;
/// when it doesn't, it's shown why. While focused, the {@link SuggestionPopup} offers what could be typed.
///
/// For an optional value, left empty means left out, and what that comes to is shown faintly in its place.
final class ArgumentBox extends EditBox {
    static final int TEXT_COLOR = 0xE0E0E0;
    static final int WARNING_COLOR = 0xFFD060;
    static final int INVALID_COLOR = 0xFF6060;
    private static final int SWATCH = 10;

    private final FieldArgument argument;
    private final SuggestionPopup popup;
    /// Told when it's emptied, if it's optional
    @Nullable
    private final Runnable onCleared;
    /// What it comes to when left out, if it's optional and that's known
    @Nullable
    private final JsonElement defaultValue;
    private String lastText;
    /// Why what's typed doesn't read, if it doesn't
    @Nullable
    private Component error;
    @Nullable
    private Component warning;

    /// @param onValue told each value that's typed, once read
    ArgumentBox(Font font, FieldArgument argument, SuggestionPopup popup, @Nullable JsonElement value,
                Consumer<JsonElement> onValue, Component label) {
        this(font, argument, popup, value, onValue, label, null, null);
    }

    /// @param onCleared    told when it's emptied, for an optional value, which is then left out; null if it's needed
    /// @param defaultValue what an optional value comes to when left out, if known
    ArgumentBox(Font font, FieldArgument argument, SuggestionPopup popup, @Nullable JsonElement value,
                Consumer<JsonElement> onValue, Component label, @Nullable Runnable onCleared,
                @Nullable JsonElement defaultValue) {
        super(font, 0, 0, argument.width(), 16, label);
        this.argument = argument;
        this.popup = popup;
        this.onCleared = onCleared;
        this.defaultValue = defaultValue;
        setMaxLength(4096);
        Component hint = onCleared != null && defaultValue != null
                ? Component.literal(argument.format(defaultValue)) : argument.hint();
        setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));
        setValue(argument.format(value));
        lastText = getValue();
        check();
        // Also told when the cursor moves, as the suggestions are of what's before it
        setResponder(text -> {
            if (!text.equals(lastText)) {
                lastText = text;
                JsonElement read = check();
                if (isLeftOut()) onCleared.run();
                else if (read != null) onValue.accept(read);
            }
            popup.update(this);
        });
    }

    /// Whether it's optional and empty, so left out
    private boolean isLeftOut() {
        return onCleared != null && getValue().isBlank();
    }

    /// What to know about what may be typed: what leaving it empty comes to, if it may be left empty, and what the
    /// argument says
    Optional<Component> info() {
        List<Component> parts = new ArrayList<>();
        if (onCleared != null) {
            parts.add(defaultValue == null ? Component.translatable("genesis.editor.default.unknown")
                    : Component.translatable("genesis.editor.default", argument.format(defaultValue)));
        }
        argument.info().ifPresent(parts::add);
        if (parts.isEmpty()) return Optional.empty();
        return Optional.of(ComponentUtils.formatList(parts, Component.literal(" · ")));
    }

    FieldArgument argument() {
        return argument;
    }

    @Nullable
    Component error() {
        return error;
    }

    /// Something to point out about what's typed, though it'll do
    @Nullable
    Component warning() {
        return warning;
    }

    /// Reads what's typed, showing whether it'll do
    ///
    /// @return what it reads as, if it does
    @Nullable
    private JsonElement check() {
        JsonElement value = null;
        if (isLeftOut()) {
            error = null;
            warning = null;
            setTextColor(TEXT_COLOR);
            return null;
        }
        try {
            value = argument.parseAll(getValue());
            error = null;
            warning = argument.warning(value).orElse(null);
        } catch (CommandSyntaxException e) {
            error = describe(e);
            warning = null;
        }
        // Why is shown under it while it's typed in, by the suggestions
        setTextColor(error != null ? INVALID_COLOR : warning != null ? WARNING_COLOR : TEXT_COLOR);
        return value;
    }

    /// The error's message, without the command-style excerpt of where it is, which a box this narrow shows anyway
    static Component describe(CommandSyntaxException e) {
        return ComponentUtils.fromMessage(e.getRawMessage());
    }

    @Override
    public void setFocused(boolean focused) {
        boolean was = isFocused();
        super.setFocused(focused);
        if (was != isFocused()) popup.focusChanged(this, isFocused());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        OptionalInt color = argument.color(getValue());
        if (color.isEmpty()) return;
        // A swatch of the colour typed, inside the box's right end
        int x = getRight() - SWATCH - 3;
        int y = getY() + (getHeight() - SWATCH) / 2;
        guiGraphics.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | color.getAsInt());
    }

    @Override
    public int getInnerWidth() {
        // Leaving room for the swatch, when there's one
        return argument.color("#000000").isPresent() ? super.getInnerWidth() - SWATCH - 4 : super.getInnerWidth();
    }

    /// Replaces what's typed, as when a suggestion's taken, with the cursor after it
    void replace(String text) {
        setValue(text);
        moveCursorToEnd(false);
    }
}
