package shipwrights.genesis.space.client.map.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.TextureSlot;
import shipwrights.genesis.space.client.map.OfflineCelestials;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/// The editor down the right of the solar system map, for making new celestials: picks which of the project's
/// celestials to edit, edits each of its documents with a form built from its codec, takes its textures, and exports
/// the lot as one pack.
///
/// Kept by the map screen across its widgets being made afresh, as when the window's resized.
public final class EditorPanel {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int WIDTH = 230;
    /// How long after the last change the project's saved
    private static final long SAVE_DELAY_MS = 1000;
    private static final int THUMBNAIL = 16;
    /// How long the delete or reset button waits to be clicked again, to confirm
    private static final long CONFIRM_MS = 3000;
    /// The most lines of problems the export button's tooltip lists
    private static final int MAX_PROBLEM_LINES = 12;
    /// Changes closer together than this are undone together, as the letters of a word typed, or a slider dragged
    private static final long STEP_MS = 1000;
    /// How many steps can be undone, at most
    private static final int MAX_UNDO = 100;
    private static final SystemToast.SystemToastIds TOAST = SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;
    /// What new celestials are called, numbered if a celestial has it already
    private static final ResourceLocation NEW_ID = ResourceLocation.fromNamespaceAndPath("example", "new_celestial");
    private static final Component ID_TOOLTIP = Component.translatable("genesis.editor.id.tooltip");

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Screen screen;
    private final CelestialProject project;
    private final TexturePreviews previews = new TexturePreviews();
    /// Told whenever the project changes, so the map draws it as it now is
    private final Runnable onChange;
    /// Makes the screen's widgets afresh, the panel's among them, as when what they show changes
    private final Runnable rebuildWidgets;
    /// Told the celestial now being edited, whenever the panel's opened or moves on to another, so the map can show it
    private final Consumer<ResourceLocation> onFocus;
    private final Path exports;
    /// What exports are named after: the project's folder
    private final String exportName;

    /// Starts hidden, leaving the whole screen to the map until the editor is wanted
    private boolean open = false;
    @Nullable
    private ResourceLocation selected;
    /// Which tab: one per document, then the textures
    private int tab = 0;
    private boolean dirty;
    private long lastChange;

    private int left;
    private int height;
    private FormList form;
    /// Edits the id of the celestial being edited, if it's a new one
    @Nullable
    private EditBox idBox;
    /// Whether to focus the id box once it's made, as for a celestial just made
    private boolean focusIdBox;
    /// Deletes the celestial being edited, or resets it if it's one of the game's own
    private Button removeButton;
    /// The celestial whose delete or reset button's been clicked once, waiting to be clicked again, and since when
    @Nullable
    private ResourceLocation armed;
    private long armedAt;
    private final Map<FormList.Row, TextureSlot> textureRows = new HashMap<>();
    /// Suggests what to type in the form's boxes
    private final SuggestionPopup popup = new SuggestionPopup(minecraft.font,
            () -> form == null ? ScreenRectangle.empty() : form.getRectangle());
    /// Makes textures that can be generated, under their rows
    private final TextureGeneratorForm generator;

    /// The project as it was before each step that can be undone, the latest first, and as it was after each step
    /// undone, to redo it
    private final Deque<Step> undoSteps = new ArrayDeque<>();
    private final Deque<Step> redoSteps = new ArrayDeque<>();
    /// The project as it was after the last step finished
    private Step committed;
    /// Whether changes are being made, as one step that hasn't finished yet
    private boolean stepOpen;
    private Button undoButton;
    private Button redoButton;

    /// The project at some moment, and the celestial being edited then
    private record Step(CelestialProject.Snapshot snapshot, @Nullable ResourceLocation selected) {}

    public EditorPanel(Screen screen, OfflineCelestials builtIn, EditorProjects.Project saved, Path exports,
                       Runnable onChange, Runnable rebuildWidgets, Consumer<ResourceLocation> onFocus) {
        this.screen = screen;
        this.onChange = onChange;
        this.rebuildWidgets = rebuildWidgets;
        this.onFocus = onFocus;
        this.project = CelestialProject.load(saved.workspace(), Templates.defaults(builtIn));
        this.exports = exports;
        this.exportName = saved.fileName();
        this.selected = project.celestials().isEmpty() ? null : project.celestials().get(0).id();
        this.generator = new TextureGeneratorForm(project, popup, this::takeGenerated);
        this.committed = new Step(project.snapshot(), selected);
        previews.sync(project.textures());
    }

    public CelestialProject project() {
        return project;
    }

    public boolean isOpen() {
        return open;
    }

    /// Where the panel starts, from the left of the screen; the map's to the left of it
    public int left(int screenWidth) {
        return open ? screenWidth - WIDTH : screenWidth;
    }

    @Nullable
    public ResourceLocation selected() {
        return selected;
    }

    /// Adds a widget to the screen, as {@link Screen#addRenderableWidget} does. Not just a consumer of widgets, as the
    /// form's list isn't one.
    public interface WidgetAdder {
        <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget);
    }

    /// Lays out the panel's widgets, handing each to {@code add}
    public void init(int screenWidth, int screenHeight, WidgetAdder add) {
        left = screenWidth - WIDTH;
        height = screenHeight;
        // The boxes it was suggesting for are gone
        popup.clear();

        add.add(Button.builder(Component.literal(open ? "»" : "«"), button -> {
                    open = !open;
                    if (open) focusSelected();
                    rebuildWidgets.run();
                })
                .bounds((open ? left : screenWidth) - 22, 4, 18, 18)
                .tooltip(Tooltip.create(Component.translatable(open ? "genesis.editor.hide" : "genesis.editor.show")))
                .build());
        if (!open) return;

        int inner = left + 6;
        int innerWidth = WIDTH - 12;

        Button previous = Button.builder(Component.literal("<"), button -> step(-1)).bounds(inner, 6, 16, 16).build();
        Button next = Button.builder(Component.literal(">"), button -> step(1)).bounds(inner + innerWidth - 16, 6, 16, 16).build();
        previous.active = next.active = project.celestials().size() > 1;
        add.add(previous);
        add.add(next);

        int half = (innerWidth - 4) / 2;
        // The game's own celestials keep their ids; a new one's can be typed over
        idBox = null;
        if (current().isPresent() && !project.isBuiltIn(current().get())) {
            idBox = idBox(inner + 20, innerWidth - 40);
            add.add(idBox);
            if (focusIdBox) {
                // Picked out, to be typed straight over
                screen.setFocused(idBox);
                idBox.moveCursorToEnd();
                idBox.setHighlightPos(0);
            }
        }
        focusIdBox = false;

        add.add(Button.builder(Component.translatable("genesis.editor.new"), button -> createNew())
                .bounds(inner, 26, half, 18).build());
        // The game's own celestials can only be put back as they were, not deleted
        boolean builtIn = current().map(project::isBuiltIn).orElse(false);
        // Still waiting to be confirmed only if it's the same celestial
        if (armed != null && !armed.equals(selected)) armed = null;
        removeButton = Button.builder(removeLabel(builtIn), button -> clickRemove())
                .bounds(inner + half + 4, 26, half, 18)
                .tooltip(builtIn ? Tooltip.create(Component.translatable("genesis.editor.reset.tooltip")) : null)
                .build();
        updateRemoveButton();
        add.add(removeButton);

        int tabs = CelestialDocuments.ALL.size() + 1;
        int tabWidth = (innerWidth - (tabs - 1) * 2) / tabs;
        for (int i = 0; i < tabs; i++) {
            int index = i;
            Component name = i < CelestialDocuments.ALL.size() ? CelestialDocuments.ALL.get(i).title()
                    : Component.translatable("genesis.editor.textures");
            Button tabButton = Button.builder(name, button -> {
                tab = index;
                rebuildWidgets.run();
            }).bounds(inner + i * (tabWidth + 2), 48, tabWidth, 18).build();
            tabButton.active = tab != i && current().isPresent();
            add.add(tabButton);
        }

        // Down to just above the undo and redo buttons
        form = new FormList(minecraft, WIDTH - 4, Math.max(20, screenHeight - 70 - 28), 70);
        form.setLeftPos(left + 2);
        add.add(form);
        showTab();

        undoButton = Button.builder(Component.translatable("genesis.editor.undo"), button -> undo())
                .bounds(inner, screenHeight - 24, half, 20).build();
        redoButton = Button.builder(Component.translatable("genesis.editor.redo"), button -> redo())
                .bounds(inner + half + 4, screenHeight - 24, half, 20).build();
        add.add(undoButton);
        add.add(redoButton);
        updateUndoButtons();
    }

    /// A box editing the id of the celestial being edited, which takes each id typed that'll do straight away. Put
    /// back to the id it has once it's left with one that won't.
    private EditBox idBox(int x, int width) {
        EditBox box = new EditBox(minecraft.font, x, 6, width, 16, Component.translatable("genesis.editor.id")) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (!focused && selected != null && !getValue().equals(selected.toString())) setValue(selected.toString());
            }
        };
        box.setMaxLength(128);
        box.setValue(String.valueOf(selected));
        box.setResponder(text -> {
            Optional<Component> problem = idProblem(text);
            box.setTextColor(problem.isPresent() ? ArgumentBox.INVALID_COLOR : ArgumentBox.TEXT_COLOR);
            box.setTooltip(Tooltip.create(problem.orElse(ID_TOOLTIP)));
            if (problem.isEmpty()) rename(ResourceLocation.parse(text));
        });
        box.setTooltip(Tooltip.create(ID_TOOLTIP));
        return box;
    }

    /// Why {@code text} won't do as the id of the celestial being edited, if it won't
    private Optional<Component> idProblem(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null || !text.contains(":") || id.getPath().isEmpty()) {
            return Optional.of(Component.translatable("genesis.editor.id.invalid"));
        }
        if (!id.equals(selected) && project.get(id).isPresent()) {
            return Optional.of(Component.translatable("genesis.editor.id.taken"));
        }
        return Optional.empty();
    }

    private void rename(ResourceLocation id) {
        Optional<ProjectCelestial> celestial = current();
        if (celestial.isEmpty() || id.equals(selected)) return;
        project.rename(celestial.get(), id);
        selected = id;
        changed();
        // Its textures are looked for somewhere else now. Only the form's made afresh, leaving the box being typed in.
        if (form != null) form.requestRebuild();
    }

    private Optional<ProjectCelestial> current() {
        return selected == null ? Optional.empty() : project.get(selected);
    }

    /// Edits {@code id}, if it's one of the project's celestials
    public boolean select(ResourceLocation id) {
        if (project.get(id).isEmpty()) return false;
        selected = id;
        open = true;
        focusSelected();
        rebuildWidgets.run();
        return true;
    }

    private void focusSelected() {
        if (selected != null) onFocus.accept(selected);
    }

    private void step(int by) {
        List<ProjectCelestial> celestials = project.celestials();
        if (celestials.isEmpty()) return;
        int index = 0;
        for (int i = 0; i < celestials.size(); i++) {
            if (celestials.get(i).id().equals(selected)) index = i;
        }
        selected = celestials.get(Math.floorMod(index + by, celestials.size())).id();
        focusSelected();
        rebuildWidgets.run();
    }

    private void showTab() {
        textureRows.clear();
        Optional<ProjectCelestial> celestial = current();
        if (celestial.isEmpty()) {
            form.show(() -> List.of(new FormList.Row(minecraft.font, Component.translatable("genesis.editor.empty"), 0, null, List.of(), 0x909090)));
            return;
        }
        ProjectCelestial edited = celestial.get();
        if (tab < CelestialDocuments.ALL.size()) {
            CelestialDocuments.Document<?> document = CelestialDocuments.ALL.get(tab);
            form.show(() -> SchemaRows.build(minecraft.font, document, () -> edited.json(document),
                    json -> edited.setJson(document, json), this::changed, form::requestRebuild, this::ids, popup));
        } else {
            form.show(() -> textureRows(edited));
        }
    }

    /// The ids there are in {@code registry}, to offer for fields naming one. Celestials are those of the game's packs
    /// and the project's, but not the one being edited, which can't refer to itself; other registries, whatever's
    /// built into the game.
    private List<ResourceLocation> ids(ResourceKey<? extends Registry<?>> registry) {
        return project.ids(registry, selected);
    }

    // Changes

    private void changed() {
        if (!stepOpen) {
            undoSteps.push(committed);
            if (undoSteps.size() > MAX_UNDO) undoSteps.removeLast();
            redoSteps.clear();
            stepOpen = true;
        }
        disarm();
        dirty = true;
        lastChange = Util.getMillis();
        previews.sync(project.textures());
        updateRemoveButton();
        updateUndoButtons();
        onChange.run();
    }

    /// Whether there's anything to export, and nothing wrong with any of it
    public boolean isExportable() {
        return project.isExportable();
    }

    /// What export does, or why it can't yet: each celestial's problems, under its id
    public Component exportTooltip() {
        List<Component> lines = new ArrayList<>();
        for (ProjectCelestial celestial : project.celestials()) {
            List<Component> problems = project.problems(celestial);
            if (problems.isEmpty()) continue;
            lines.add(Component.literal(celestial.id().toString()).withStyle(ChatFormatting.YELLOW));
            problems.forEach(problem -> lines.add(Component.literal("  ").append(problem).withStyle(ChatFormatting.RED)));
        }
        if (lines.isEmpty()) {
            return Component.translatable(isExportable() ? "genesis.editor.export.tooltip" : "genesis.editor.export.nothing");
        }
        if (lines.size() > MAX_PROBLEM_LINES) {
            int more = lines.size() - MAX_PROBLEM_LINES;
            lines.subList(MAX_PROBLEM_LINES, lines.size()).clear();
            lines.add(Component.translatable("genesis.editor.export.more", more).withStyle(ChatFormatting.GRAY));
        }
        return ComponentUtils.formatList(lines, Component.literal("\n"));
    }

    /// A message in the corner, as of an export
    private void toast(Component title, Component detail) {
        minecraft.getToasts().addToast(SystemToast.multiline(minecraft, TOAST, title, detail));
    }

    /// Saves the project once it's been left alone for a moment
    public void tick() {
        if (armed != null && Util.getMillis() - armedAt >= CONFIRM_MS) disarm();
        if (stepOpen && Util.getMillis() - lastChange >= STEP_MS) finishStep();
        if (dirty && Util.getMillis() - lastChange >= SAVE_DELAY_MS) save();
    }

    private void save() {
        project.save();
        dirty = false;
    }

    /// Saves any changes straight away, without waiting for them to be left alone, as when the game's stopping
    public void saveNow() {
        if (dirty) save();
    }

    /// Saves the project, and takes the textures previewed away again
    public void close() {
        generator.stopAll();
        saveNow();
        previews.releaseAll();
    }

    // Undo

    /// Ends the step being made, if one is, so what's done next is undone apart from it
    private void finishStep() {
        if (!stepOpen) return;
        committed = new Step(project.snapshot(), selected);
        stepOpen = false;
    }

    private void undo() {
        finishStep();
        if (undoSteps.isEmpty()) return;
        redoSteps.push(committed);
        committed = undoSteps.pop();
        restore(committed);
    }

    private void redo() {
        finishStep();
        if (redoSteps.isEmpty()) return;
        undoSteps.push(committed);
        committed = redoSteps.pop();
        restore(committed);
    }

    /// Puts the project back as it was at {@code step}, still editing the same celestial if it's there, or else the
    /// one being edited then
    private void restore(Step step) {
        generator.reset();
        project.restore(step.snapshot());
        if (selected == null || project.get(selected).isEmpty()) {
            selected = step.selected() != null && project.get(step.selected()).isPresent() ? step.selected()
                    : project.celestials().isEmpty() ? null : project.celestials().get(0).id();
            focusSelected();
        }
        armed = null;
        dirty = true;
        lastChange = Util.getMillis();
        previews.sync(project.textures());
        onChange.run();
        // The form was editing celestials that have been made afresh
        rebuildWidgets.run();
    }

    private void updateUndoButtons() {
        if (undoButton == null) return;
        undoButton.active = stepOpen || !undoSteps.isEmpty();
        redoButton.active = !stepOpen && !redoSteps.isEmpty();
    }

    // Creating and deleting

    /// Makes a blank celestial with an id of its own, and edits it, with its id picked out to be typed over
    private void createNew() {
        finishStep();
        ResourceLocation id = project.unusedId(NEW_ID);
        project.add(Templates.blankCelestial(id));
        selected = id;
        tab = 0;
        focusIdBox = true;
        changed();
        focusSelected();
        rebuildWidgets.run();
    }

    /// Only active when there's something to delete or reset
    private void updateRemoveButton() {
        if (removeButton == null) return;
        Optional<ProjectCelestial> celestial = current();
        removeButton.active = celestial.isPresent() && (!project.isBuiltIn(celestial.get()) || project.isChanged(celestial.get()));
    }

    /// "Delete" or "Reset", or once clicked, asking to be clicked again
    private Component removeLabel(boolean builtIn) {
        String key = builtIn ? "genesis.editor.reset" : "genesis.editor.delete";
        return armed == null ? Component.translatable(key)
                : Component.translatable(key + ".confirm").withStyle(ChatFormatting.RED);
    }

    /// Clicked once, asks to be clicked again; clicked again soon after, deletes the celestial being edited, or for one
    /// of the game's own, puts it back as it was
    private void clickRemove() {
        Optional<ProjectCelestial> celestial = current();
        if (celestial.isEmpty()) return;
        boolean builtIn = project.isBuiltIn(celestial.get());
        if (!celestial.get().id().equals(armed)) {
            armed = celestial.get().id();
            armedAt = Util.getMillis();
            removeButton.setMessage(removeLabel(builtIn));
            return;
        }
        armed = null;
        finishStep();
        if (builtIn) {
            project.reset(celestial.get());
        } else {
            project.remove(celestial.get());
            selected = project.celestials().isEmpty() ? null : project.celestials().get(0).id();
            focusSelected();
        }
        changed();
        // Its form shows what was there before
        rebuildWidgets.run();
    }

    /// No longer waiting for the delete or reset button to be clicked again
    private void disarm() {
        if (armed == null) return;
        armed = null;
        if (removeButton != null) {
            removeButton.setMessage(removeLabel(current().map(project::isBuiltIn).orElse(false)));
        }
    }

    // Textures

    private List<FormList.Row> textureRows(ProjectCelestial celestial) {
        Font font = minecraft.font;
        List<FormList.Row> rows = new ArrayList<>();
        textureRows.clear();
        List<TextureSlot> slots = celestial.textureSlots();
        if (slots.isEmpty()) {
            Component reason = celestial.appearance().isEmpty()
                    ? Component.translatable("genesis.editor.textures.invalid_appearance")
                    : Component.translatable("genesis.editor.textures.none");
            rows.add(new FormList.Row(font, reason, 0, null, List.of(), 0x909090));
            return rows;
        }
        for (TextureSlot slot : slots) {
            boolean has = project.textures().containsKey(slot.location());
            Button browse = Button.builder(Component.translatable("genesis.editor.textures.browse"), button -> browse(slot))
                    .size(52, 16).build();
            // The game's own texture can't be taken away, only put back once replaced
            boolean builtInTexture = project.hasDefaultTexture(slot.location());
            Button clear = Button.builder(Component.literal("✕"), button -> {
                        finishStep();
                        generator.stopLive(slot.location());
                        if (builtInTexture) project.resetTexture(slot.location());
                        else project.textures().remove(slot.location());
                        changed();
                        form.requestRebuild();
                    }).size(16, 16)
                    .tooltip(Tooltip.create(Component.translatable(builtInTexture
                            ? "genesis.editor.textures.reset" : "genesis.editor.textures.clear")))
                    .build();
            // Greyed out when there's nothing to take away or put back
            clear.active = has && (!builtInTexture || project.isChangedTexture(slot.location()));

            List<AbstractWidget> buttons = new ArrayList<>(List.of(browse));
            boolean generating = slot.generatable() && generator.isOpen(slot.location());
            if (slot.generatable()) {
                buttons.add(Button.builder(Component.translatable(generating
                                        ? "genesis.editor.textures.generate.hide" : "genesis.editor.textures.generate"),
                                button -> {
                                    // What generating replaces can be had back on its own
                                    finishStep();
                                    generator.toggle(slot.location());
                                    form.requestRebuild();
                                })
                        .size(56, 16)
                        .tooltip(Tooltip.create(Component.translatable("genesis.editor.textures.generate.tooltip")))
                        .build());
            }
            buttons.add(clear);

            Component name = slot.required() ? slot.name().copy().append(" *") : slot.name();
            FormList.Row row = new FormList.Row(font, name, 0, null, buttons)
                    .painted((graphics, rowFont, x, top, width, rowHeight) -> {
                        int y = top + (rowHeight - THUMBNAIL) / 2;
                        if (has || TextureSlot.exists(slot.location())) {
                            graphics.blit(slot.location(), x, y, 0, 0, THUMBNAIL, THUMBNAIL, THUMBNAIL, THUMBNAIL);
                        } else {
                            graphics.renderOutline(x, y, THUMBNAIL, THUMBNAIL, 0xFF606060);
                        }
                    }, THUMBNAIL + 4);
            textureRows.put(row, slot);
            rows.add(row);

            previews.size(slot.location()).ifPresent(size -> rows.add(new FormList.Row(font,
                    Component.translatable("genesis.editor.textures.size", size.width(), size.height()), 1, null, List.of(), 0x909090)));
            if (generating) rows.addAll(generator.rows(font, slot.location(), form::requestRebuild));
        }
        return rows;
    }

    private void browse(TextureSlot slot) {
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            path = TinyFileDialogs.tinyfd_openFileDialog(
                    Component.translatable("genesis.editor.textures.browse_title", slot.name()).getString(),
                    null, filters, "PNG", false);
        }
        if (path != null) importTexture(slot, Path.of(path));
    }

    /// Puts dropped PNGs in place: the first in the texture row under the cursor, if any, or else in the first slot
    /// without one
    public boolean onFilesDrop(List<Path> paths, double mouseX, double mouseY) {
        Optional<ProjectCelestial> celestial = current();
        if (!open || celestial.isEmpty() || paths.isEmpty()) return false;
        Optional<Path> png = paths.stream().filter(path -> path.toString().toLowerCase().endsWith(".png")).findFirst();
        if (png.isEmpty()) return false;

        TextureSlot slot = null;
        if (tab == CelestialDocuments.ALL.size()) slot = textureRows.get(form.rowAt(mouseX, mouseY));
        if (slot == null) {
            List<TextureSlot> slots = celestial.get().textureSlots();
            slot = slots.stream().filter(s -> !project.textures().containsKey(s.location())).findFirst()
                    .orElse(slots.isEmpty() ? null : slots.get(0));
        }
        if (slot == null) return false;
        importTexture(slot, png.get());
        if (tab != CelestialDocuments.ALL.size()) {
            tab = CelestialDocuments.ALL.size();
            rebuildWidgets.run();
        }
        return true;
    }

    private void importTexture(TextureSlot slot, Path path) {
        try {
            byte[] png = Files.readAllBytes(path);
            // Read it now, so a file that isn't a PNG is turned away rather than kept
            TexturePreviews.readPng(png).close();
            finishStep();
            // Not to be generated over
            generator.stopLive(slot.location());
            project.textures().put(slot.location(), png);
            changed();
            form.requestRebuild();
        } catch (IOException e) {
            LOGGER.warn("Couldn't read {} as a PNG", path, e);
            toast(Component.translatable("genesis.editor.textures.not_png"), Component.literal(path.getFileName().toString()));
        }
    }

    /// Puts a texture just generated in place, remembering how it was made
    private void takeGenerated(ResourceLocation location, byte[] png, PlanetTextureSettings settings) {
        boolean had = project.textures().containsKey(location);
        boolean wasChanged = project.isChangedTexture(location);
        boolean resized = previews.size(location)
                .map(size -> size.width() != settings.width() || size.height() != settings.height())
                .orElse(true);
        project.textures().put(location, png);
        project.generators().put(location, settings);
        changed();
        // Only when its row shows something else, so that a colour being picked isn't interrupted
        if (form != null && (!had || resized || wasChanged != project.isChangedTexture(location))) form.requestRebuild();
    }

    // Export

    /// Saves the project, then zips it up into the exports folder, and shows it there
    public void export() {
        try {
            save();
            Path zip = project.export(exports, exportName);
            toast(Component.translatable("genesis.editor.exported"), Component.literal(zip.getFileName().toString()));
            Util.getPlatform().openFile(exports.toFile());
        } catch (IOException e) {
            LOGGER.error("Couldn't export the celestial editor's project", e);
            toast(Component.translatable("genesis.editor.export_failed"), Component.literal(String.valueOf(e.getMessage())));
        }
    }

    // Drawing

    /// The panel's background and text; its widgets draw themselves
    public void render(GuiGraphics guiGraphics) {
        if (!open) return;
        Font font = minecraft.font;
        guiGraphics.fill(left, 0, left + WIDTH, height, 0xC0101018);
        guiGraphics.fill(left, 0, left + 1, height, 0xFF303040);

        // A new celestial's id is in its box
        if (idBox != null) return;
        Component name = selected == null ? Component.translatable("genesis.editor.none") : Component.literal(selected.toString());
        int nameWidth = WIDTH - 12 - 40;
        FormattedCharSequence nameText = font.width(name) <= nameWidth ? name.getVisualOrderText()
                : font.split(name, nameWidth).get(0);
        guiGraphics.drawCenteredString(font, nameText, left + WIDTH / 2, 10, 0xFFFFFF);
    }

    /// What's drawn over everything else: suggestions for the box being typed in
    public void renderOverlay(GuiGraphics guiGraphics) {
        if (open) popup.render(guiGraphics);
    }

    /// Input for the suggestions, before the widgets under them get it
    public boolean keyPressed(int keyCode) {
        return open && popup.keyPressed(keyCode);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        return open && popup.mouseClicked(mouseX, mouseY);
    }

    /// Dragging on the suggestions, as to pick a colour, before anything under them gets it
    public boolean mouseDragged(double mouseX, double mouseY) {
        return open && popup.mouseDragged(mouseX, mouseY);
    }

    public boolean mouseReleased() {
        return open && popup.mouseReleased();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return open && popup.mouseScrolled(mouseX, mouseY, scrollY);
    }

    /// Celestials as the project has them, for the map to draw: by id, those whose data and appearance can be read
    public Map<ResourceLocation, Celestial> celestials() {
        Map<ResourceLocation, Celestial> celestials = new HashMap<>();
        for (ProjectCelestial celestial : project.celestials()) {
            celestial.decode(CelestialDocuments.DATA).result().ifPresent(data -> celestials.put(celestial.id(), data));
        }
        return celestials;
    }

    public Map<ResourceLocation, CelestialVisualizer> appearances() {
        Map<ResourceLocation, CelestialVisualizer> appearances = new HashMap<>();
        for (ProjectCelestial celestial : project.celestials()) {
            celestial.appearance().ifPresent(appearance -> appearances.put(celestial.id(), appearance));
        }
        return appearances;
    }
}
