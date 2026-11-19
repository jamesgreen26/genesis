package shipwrights.genesis.space.client.map;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import shipwrights.genesis.space.Time;

import java.util.List;
import java.util.function.Consumer;

/// The map's clock, and the strip in the top-left that runs it: slower (or backwards), play or pause, faster, and a
/// readout of the time and speed, which can be dragged to scrub through time or clicked to go back to the start.
///
/// Speeds are of in-game time each real second, from a ladder covering a planet's day up to its year. Stepping slower
/// past the slowest goes backwards, to rewind past something rather than wait for it to come round again. Time never
/// goes before the start, the 0 tick a world's day time starts at.
final class TimeControls {
    /// A speed on the ladder, in ticks each real second
    record Speed(double ticksPerSecond, Component name) {}

    static final List<Speed> SPEEDS = List.of(
            new Speed(20, Component.translatable("genesis.solar_system.speed.real_time")),
            new Speed(1_000, Component.translatable("genesis.solar_system.speed.hour")),
            new Speed(24_000, Component.translatable("genesis.solar_system.speed.day")),
            new Speed(168_000, Component.translatable("genesis.solar_system.speed.week")),
            new Speed(720_000, Component.translatable("genesis.solar_system.speed.month")));

    private static final int TICKS_PER_DAY = 24_000;
    private static final int TICKS_PER_HOUR = 1_000;
    /// The 0 tick is at sunrise, 06:00, as in vanilla
    private static final int START_HOUR = 6;
    /// The longest a frame's taken to count for, so time doesn't leap after a hitch
    private static final double MAX_FRAME_SECONDS = 0.25;
    /// How many seconds' worth of the current speed one GUI pixel of dragging the readout scrubs through
    private static final double SCRUB_SECONDS_PER_PIXEL = 0.02;

    private static final int BUTTON = 16;
    private static final int READOUT_WIDTH = 150;

    private double ticks;
    private boolean paused = true;
    /// Which speed, counting from 1 at the slowest: negative to go backwards. Kept while paused.
    private int step = 1;
    private long lastNanos = -1;

    private Button playButton;

    /// The time it is on the map now
    Time time() {
        long whole = (long) Math.floor(ticks);
        return Time.of(whole, (float) (ticks - whole));
    }

    private Speed speed() {
        return SPEEDS.get(Math.abs(step) - 1);
    }

    /// Ticks each real second, backwards if negative; nothing while paused
    private double velocity() {
        return paused ? 0 : Math.signum(step) * speed().ticksPerSecond();
    }

    /// Moves time on by as long as it's been since it was last called, at the speed it's going
    void advance() {
        long now = Util.getNanos();
        if (lastNanos >= 0) {
            double seconds = Math.min((now - lastNanos) / 1e9, MAX_FRAME_SECONDS);
            setTicks(ticks + velocity() * seconds);
        }
        lastNanos = now;
    }

    /// Never before the start. Rewinding there stops, ready to play forwards again at the same speed.
    private void setTicks(double to) {
        ticks = to;
        if (ticks < 0) {
            ticks = 0;
            if (!paused && step < 0) {
                paused = true;
                step = -step;
                refresh();
            }
        }
    }

    // Controls

    void togglePause() {
        paused = !paused;
        refresh();
    }

    /// Faster forwards, or slower backwards; from paused, plays forwards in real time
    void faster() {
        if (paused) {
            paused = false;
            step = 1;
        } else {
            step = step == -1 ? 1 : Math.min(step + 1, SPEEDS.size());
        }
        refresh();
    }

    /// Slower forwards, or faster backwards; from paused, plays backwards in real time
    void slower() {
        if (paused) {
            paused = false;
            step = -1;
        } else {
            step = step == 1 ? -1 : Math.max(step - 1, -SPEEDS.size());
        }
        refresh();
    }

    /// Back to the start, Day 1 at 06:00
    void reset() {
        ticks = 0;
    }

    // Widgets

    /// Lays out the strip in the top-left corner, handing each widget to {@code add}
    void init(Consumer<AbstractWidget> add) {
        int x = 4, y = 4;
        add.accept(Button.builder(Component.literal("«"), button -> slower())
                .bounds(x, y, BUTTON, BUTTON)
                .tooltip(Tooltip.create(Component.translatable("genesis.solar_system.slower")))
                .build());
        playButton = Button.builder(Component.empty(), button -> togglePause())
                .bounds(x + BUTTON + 2, y, BUTTON, BUTTON)
                .build();
        add.accept(playButton);
        add.accept(Button.builder(Component.literal("»"), button -> faster())
                .bounds(x + 2 * (BUTTON + 2), y, BUTTON, BUTTON)
                .tooltip(Tooltip.create(Component.translatable("genesis.solar_system.faster")))
                .build());
        add.accept(new Readout(x + 3 * (BUTTON + 2) + 2, y, READOUT_WIDTH, BUTTON));
        refresh();
    }

    /// Shows on the play button whether it'll play or pause
    private void refresh() {
        if (playButton == null) return;
        playButton.setMessage(Component.literal(paused ? "▶" : "||"));
        playButton.setTooltip(Tooltip.create(Component.translatable(
                paused ? "genesis.solar_system.play" : "genesis.solar_system.pause")));
    }

    /// The time as a clock shows it, as "Day 12, 06:00"
    private Component clock() {
        long minutesSinceMidnight = (long) Math.floor((ticks + START_HOUR * TICKS_PER_HOUR) * 60 / TICKS_PER_HOUR);
        long day = minutesSinceMidnight / (24 * 60) + 1;
        long hour = minutesSinceMidnight / 60 % 24;
        long minute = minutesSinceMidnight % 60;
        return Component.translatable("genesis.solar_system.clock", day, String.format("%02d:%02d", hour, minute));
    }

    /// The speed, greyed out while paused
    private Component speedName() {
        Component name = step < 0 ? Component.translatable("genesis.solar_system.speed.reverse", speed().name())
                : speed().name();
        return paused ? name.copy().withStyle(ChatFormatting.GRAY) : name;
    }

    /// The time and speed. Dragged sideways, scrubs through time, faster the faster it's going; clicked without
    /// dragging, goes back to the start.
    private final class Readout extends AbstractWidget {
        private double dragged;

        Readout(int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("genesis.solar_system.clock.narration"));
            setTooltip(Tooltip.create(Component.translatable("genesis.solar_system.clock.tooltip")));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), isHoveredOrFocused() ? 0xA0303040 : 0x80000000);
            Font font = Minecraft.getInstance().font;
            Component text = clock().copy().append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(speedName());
            guiGraphics.drawString(font, text, getX() + 5, getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            dragged = 0;
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            dragged += Math.abs(dragX);
            setTicks(ticks + dragX * speed().ticksPerSecond() * SCRUB_SECONDS_PER_PIXEL);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            if (dragged < 2) reset();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
