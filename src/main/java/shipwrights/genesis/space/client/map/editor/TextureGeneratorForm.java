package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import shipwrights.genesis.space.client.texture.PlanetTextureGenerator;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings.Group;
import shipwrights.genesis.util.MoreCodecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleConsumer;

/// The rows under a texture that can be {@linkplain PlanetTextureGenerator generated}, for making it from noise and a
/// palette rather than a PNG: its size and seed, how its two main palettes are blended, how noisy, blocky and
/// dithered it is, and its colours.
///
/// Opening the rows makes the texture straight away, and from then on, while they're open, every change makes it
/// afresh, so the planet on the map changes as the settings do.
///
/// Textures are made off the render thread, one at a time: changes made meanwhile wait, and only the last of them is
/// made. Changing only colours reuses the last layout, so is quick enough to follow a colour being dragged.
final class TextureGeneratorForm {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COLOR_ROW_DEPTH = 2;

    /// Told each texture made, on the render thread
    interface Generated {
        void accept(ResourceLocation location, byte[] png, PlanetTextureSettings settings);
    }

    private final Minecraft minecraft = Minecraft.getInstance();
    private final CelestialProject project;
    private final SuggestionPopup popup;
    private final Generated onGenerated;

    /// The texture whose rows are showing, if any: only one at a time
    @Nullable
    private ResourceLocation open;
    /// The settings being edited for each texture, once its rows have been shown
    private final Map<ResourceLocation, PlanetTextureSettings> editing = new HashMap<>();
    /// The textures that are made afresh whenever their settings change, having been generated
    private final Set<ResourceLocation> live = new HashSet<>();

    private boolean running;
    @Nullable
    private Request pending;
    /// The last layout made, kept to paint in other colours. Only used by the task making a texture, of which there's
    /// only ever one at a time.
    @Nullable
    private volatile PlanetTextureGenerator.Layout lastLayout;
    /// Counts the times generating's been stopped, so textures that were being made then aren't put in place
    private int stops;

    private record Request(ResourceLocation location, PlanetTextureSettings settings) {}

    TextureGeneratorForm(CelestialProject project, SuggestionPopup popup, Generated onGenerated) {
        this.project = project;
        this.popup = popup;
        this.onGenerated = onGenerated;
    }

    boolean isOpen(ResourceLocation location) {
        return location.equals(open);
    }

    /// Shows the rows for {@code location} and makes its texture, hiding any others, or hides them if they're showing
    void toggle(ResourceLocation location) {
        if (isOpen(location)) {
            open = null;
            live.remove(location);
        } else {
            if (open != null) live.remove(open);
            open = location;
            generate(location);
        }
    }

    /// Stops making the texture at {@code location} afresh, as when another's put in its place
    void stopLive(ResourceLocation location) {
        live.remove(location);
        if (pending != null && pending.location().equals(location)) pending = null;
    }

    /// Stops making any texture, and drops those being made, as when the editor's closed
    void stopAll() {
        live.clear();
        pending = null;
        stops++;
    }

    /// Hides the rows, stops making any texture, and forgets the settings being edited, so they're read afresh from the
    /// project, as when it's been put back as it was
    void reset() {
        stopAll();
        open = null;
        editing.clear();
    }

    /// The settings being edited for {@code location}: those it was last generated with, or else the defaults, with a
    /// seed of its own
    PlanetTextureSettings settings(ResourceLocation location) {
        return editing.computeIfAbsent(location, key -> {
            PlanetTextureSettings saved = project.generators().get(key);
            return saved != null ? saved : PlanetTextureSettings.DEFAULT.withSeed(ThreadLocalRandom.current().nextInt(1_000_000));
        });
    }

    /// Takes {@code settings} for {@code location}, making its texture afresh if it's live
    ///
    /// @param reshape whether the rows have to be made afresh, as for a different number of colours
    private void change(ResourceLocation location, PlanetTextureSettings settings, boolean reshape, Runnable rebuild) {
        editing.put(location, settings);
        if (live.contains(location)) request(new Request(location, settings));
        if (reshape) rebuild.run();
    }

    private void generate(ResourceLocation location) {
        live.add(location);
        request(new Request(location, settings(location)));
    }

    private void request(Request request) {
        if (running) {
            pending = request;
        } else {
            start(request);
        }
    }

    private void start(Request request) {
        running = true;
        int startedAt = stops;
        PlanetTextureSettings settings = request.settings();
        CompletableFuture.supplyAsync(() -> {
            PlanetTextureGenerator.Layout layout = lastLayout;
            if (layout == null || !layout.settings().sameLayout(settings)) {
                layout = PlanetTextureGenerator.layout(settings);
                lastLayout = layout;
            }
            return PlanetTextureGenerator.png(layout.width(), layout.height(), layout.paint(settings));
        }, Util.backgroundExecutor()).whenCompleteAsync((png, error) -> {
            running = false;
            if (error != null) {
                LOGGER.error("Couldn't generate texture {}", request.location(), error);
            } else if (startedAt == stops && live.contains(request.location())) {
                onGenerated.accept(request.location(), png, settings);
            }
            Request next = pending;
            pending = null;
            if (next != null) start(next);
        }, minecraft);
    }

    // Rows

    /// The rows for the texture at {@code location}
    ///
    /// @param rebuild makes the form's rows afresh, as when what they show changes
    List<FormList.Row> rows(Font font, ResourceLocation location, Runnable rebuild) {
        List<FormList.Row> rows = new ArrayList<>();
        PlanetTextureSettings settings = settings(location);
        String key = "generator." + location + ".";

        Button sizeButton = Button.builder(Component.translatable("genesis.editor.generator.size.value", settings.faceSize()),
                        button -> {
                            List<Integer> sizes = PlanetTextureSettings.FACE_SIZES;
                            int index = sizes.indexOf(settings(location).faceSize());
                            change(location, settings(location).withFaceSize(sizes.get((index + 1) % sizes.size())), true, rebuild);
                        })
                .size(90, 16)
                .tooltip(Tooltip.create(Component.translatable("genesis.editor.generator.size.tooltip",
                        settings.faceSize(), settings.width(), settings.height())))
                .build();
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.size"), 1, null, List.of(sizeButton))
                .keyed(key + "size"));

        EditBox seedBox = new EditBox(font, 0, 0, 72, 16, Component.translatable("genesis.editor.generator.seed"));
        seedBox.setMaxLength(32);
        seedBox.setValue(Long.toString(settings.seed()));
        seedBox.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.seed.tooltip")));
        seedBox.setResponder(text -> {
            long seed = parseSeed(text);
            if (seed != settings(location).seed()) change(location, settings(location).withSeed(seed), false, rebuild);
        });
        Button reroll = Button.builder(Component.literal("↻"),
                        button -> seedBox.setValue(Integer.toString(ThreadLocalRandom.current().nextInt(1_000_000))))
                .size(16, 16)
                .tooltip(Tooltip.create(Component.translatable("genesis.editor.generator.reroll")))
                .build();
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.seed"), 1, null, List.of(seedBox, reroll))
                .keyed(key + "seed"));

        Slider blend = new Slider(90, settings.blend(), "genesis.editor.generator.blend.value",
                value -> change(location, settings(location).withBlend(value), false, rebuild));
        blend.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.blend.tooltip")));
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.blend"), 1, null, List.of(blend))
                .keyed(key + "blend"));

        Slider noisiness = new Slider(90, settings.noisiness(), "genesis.editor.generator.noisiness.value",
                value -> change(location, settings(location).withNoisiness(value), false, rebuild));
        noisiness.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.noisiness.tooltip")));
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.noisiness"), 1, null, List.of(noisiness))
                .keyed(key + "noisiness"));

        Slider blockiness = new Slider(90, settings.blockiness(), "genesis.editor.generator.blockiness.value",
                value -> change(location, settings(location).withBlockiness(value), false, rebuild));
        blockiness.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.blockiness.tooltip")));
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.blockiness"), 1, null, List.of(blockiness))
                .keyed(key + "blockiness"));

        Slider spread = new Slider(90, settings.ditherSpread(), "genesis.editor.generator.dither_spread.value",
                value -> change(location, settings(location).withDitherSpread(value), false, rebuild));
        spread.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.dither_spread.tooltip")));
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.dither_spread"), 1, null, List.of(spread))
                .keyed(key + "dither_spread"));

        Slider coverage = new Slider(90, settings.ditherCoverage(), "genesis.editor.generator.dither_coverage.value",
                value -> change(location, settings(location).withDitherCoverage(value), false, rebuild));
        coverage.setTooltip(Tooltip.create(Component.translatable("genesis.editor.generator.dither_coverage.tooltip")));
        rows.add(new FormList.Row(font, Component.translatable("genesis.editor.generator.dither_coverage"), 1, null, List.of(coverage))
                .keyed(key + "dither_coverage"));

        for (Group group : Group.values()) rows.addAll(colorRows(font, location, settings, group, key, rebuild));

        return rows;
    }

    /// A heading for {@code group}, with buttons to add and remove colours, then a box for each of its colours
    private List<FormList.Row> colorRows(Font font, ResourceLocation location, PlanetTextureSettings settings,
                                         Group group, String key, Runnable rebuild) {
        List<FormList.Row> rows = new ArrayList<>();
        List<Integer> colors = settings.colors(group);

        Button add = Button.builder(Component.literal("+"), button -> {
                    List<Integer> changed = new ArrayList<>(settings(location).colors(group));
                    changed.add(changed.isEmpty() ? firstColor(group) : changed.getLast());
                    change(location, settings(location).withColors(group, changed), true, rebuild);
                })
                .size(16, 16)
                .tooltip(Tooltip.create(Component.translatable("genesis.editor.generator.add")))
                .build();
        add.active = colors.size() < PlanetTextureSettings.MAX_COLORS;
        Button remove = Button.builder(Component.literal("−"), button -> {
                    List<Integer> changed = new ArrayList<>(settings(location).colors(group));
                    if (changed.isEmpty()) return;
                    changed.removeLast();
                    change(location, settings(location).withColors(group, changed), true, rebuild);
                })
                .size(16, 16)
                .tooltip(Tooltip.create(Component.translatable("genesis.editor.generator.remove")))
                .build();
        remove.active = colors.size() > group.minColors();

        String groupKey = "genesis.editor.generator.group." + group.getSerializedName();
        rows.add(new FormList.Row(font, Component.translatable(groupKey), 1, null, List.of(add, remove))
                .keyed(key + group.getSerializedName()));
        for (int i = 0; i < colors.size(); i++) {
            int index = i;
            Component label = Component.translatable("genesis.editor.generator.color", i + 1);
            ArgumentBox box = new ArgumentBox(font, new FieldArgument.ColorArgument(), popup,
                    new JsonPrimitive(MoreCodecs.hexColor(colors.get(i))), value -> {
                OptionalInt color = colorOf(value);
                if (color.isPresent()) {
                    change(location, settings(location).withColor(group, index, color.getAsInt()), false, rebuild);
                }
            }, label);
            rows.add(new FormList.Row(font, label, COLOR_ROW_DEPTH, null, List.of(box))
                    .keyed(key + group.getSerializedName() + "." + i));
        }
        return rows;
    }

    /// The colour a group with none starts from: the defaults' first, or white
    private static int firstColor(Group group) {
        List<Integer> colors = PlanetTextureSettings.DEFAULT.colors(group);
        return colors.isEmpty() ? 0xFFFFFF : colors.getFirst();
    }

    private static OptionalInt colorOf(JsonElement json) {
        if (!json.isJsonPrimitive()) return OptionalInt.empty();
        return FieldArgument.ColorArgument.parseColor(json.getAsString());
    }

    /// A seed typed as a number, or failing that, as words, which stand for the number they hash to
    static long parseSeed(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0;
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return trimmed.hashCode();
        }
    }

    /// A slider from 0 to 1, shown as a percentage
    private static final class Slider extends AbstractSliderButton {
        private final String messageKey;
        private final DoubleConsumer onChange;

        Slider(int width, double value, String messageKey, DoubleConsumer onChange) {
            super(0, 0, width, 16, Component.empty(), value);
            this.messageKey = messageKey;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(messageKey, Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            onChange.accept(value);
        }
    }
}
