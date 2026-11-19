package shipwrights.genesis.space.client.map;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import shipwrights.genesis.space.client.map.editor.EditorProjects;
import shipwrights.genesis.space.client.map.editor.EditorProjects.Project;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

/// The celestial editor's projects, to pick one to work on, as the game's worlds are picked: opened, made, imported
/// from an exported zip, renamed, duplicated and deleted. Reached from the title screen (see
/// {@link SolarSystemMapButton}); opening one opens the {@link SolarSystemScreen} on it.
public final class EditorProjectsScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private static final int ROW_HEIGHT = 36;

    private final Screen lastScreen;
    private final EditorProjects projects = new EditorProjects(genesisFolder().resolve("projects"));
    private ProjectList list;
    /// The folder of the project to pick out once the list's made afresh, as of one just made
    @Nullable
    private Path toSelect;

    private Button openButton;
    private Button renameButton;
    private Button duplicateButton;
    private Button deleteButton;

    public EditorProjectsScreen(Screen lastScreen) {
        super(Component.translatable("genesis.editor.projects.title"));
        this.lastScreen = lastScreen;
    }

    private static Path genesisFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("genesis");
    }

    /// Where exports go, from every project
    static Path exportsFolder() {
        return genesisFolder().resolve("exports");
    }

    @Override
    protected void init() {
        Path selected = toSelect != null ? toSelect
                : list != null && list.getSelected() != null ? list.getSelected().project.folder() : null;
        toSelect = null;
        list = addRenderableWidget(new ProjectList(minecraft, width, height - 112, 48, ROW_HEIGHT));
        for (Project project : projects.list()) {
            ProjectList.Entry entry = list.add(project);
            if (project.folder().equals(selected)) list.setSelected(entry);
        }

        int center = width / 2;
        // Along the top: what's done to a project, or to make one
        openButton = addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.open"),
                button -> selected().ifPresent(this::open)).bounds(center - 154, height - 52, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.new"), button -> create())
                .bounds(center - 76, height - 52, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.import"), button -> importZip())
                .bounds(center + 4, height - 52, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.open_folder"), button -> openFolder())
                .bounds(center + 82, height - 52, 72, 20).build());
        // Along the bottom: changes to the project picked
        renameButton = addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.rename"),
                button -> selected().ifPresent(this::rename)).bounds(center - 154, height - 28, 72, 20).build());
        duplicateButton = addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.duplicate"),
                button -> selected().ifPresent(this::duplicate)).bounds(center - 76, height - 28, 72, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("genesis.editor.projects.delete"),
                button -> selected().ifPresent(this::delete)).bounds(center + 4, height - 28, 72, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(center + 82, height - 28, 72, 20).build());
        updateButtons();
    }

    private Optional<Project> selected() {
        ProjectList.Entry entry = list.getSelected();
        return entry == null ? Optional.empty() : Optional.of(entry.project);
    }

    private void updateButtons() {
        // Not yet, while the list's being filled
        if (openButton == null) return;
        boolean any = list.getSelected() != null;
        openButton.active = renameButton.active = duplicateButton.active = deleteButton.active = any;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        if (list.children().isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("genesis.editor.projects.empty"),
                    width / 2, 48 + 20, 0x909090);
        }
    }

    // Actions

    private void open(Project project) {
        // Picked out again on coming back
        toSelect = project.folder();
        minecraft.setScreen(new SolarSystemScreen(this, project));
    }

    private void create() {
        minecraft.setScreen(new ProjectNameScreen(this, Component.translatable("genesis.editor.projects.new.title"),
                Component.translatable("genesis.editor.projects.new.default").getString(), name -> {
                    try {
                        open(projects.create(name));
                    } catch (IOException e) {
                        failed("genesis.editor.projects.new.failed", e);
                    }
                }));
    }

    private void rename(Project project) {
        minecraft.setScreen(new ProjectNameScreen(this, Component.translatable("genesis.editor.projects.rename.title"),
                project.name(), name -> {
                    try {
                        toSelect = projects.rename(project, name).folder();
                    } catch (IOException e) {
                        failed("genesis.editor.projects.rename.failed", e);
                    }
                    minecraft.setScreen(this);
                }));
    }

    private void duplicate(Project project) {
        minecraft.setScreen(new ProjectNameScreen(this, Component.translatable("genesis.editor.projects.duplicate.title"),
                Component.translatable("genesis.editor.projects.duplicate.default", project.name()).getString(), name -> {
                    try {
                        toSelect = projects.duplicate(project, name).folder();
                    } catch (IOException e) {
                        failed("genesis.editor.projects.duplicate.failed", e);
                    }
                    minecraft.setScreen(this);
                }));
    }

    private void delete(Project project) {
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                try {
                    projects.delete(project);
                } catch (IOException e) {
                    failed("genesis.editor.projects.delete.failed", e);
                }
            }
            minecraft.setScreen(this);
        }, Component.translatable("genesis.editor.projects.delete.question"),
                Component.translatable("genesis.editor.projects.delete.warning", project.name()),
                Component.translatable("genesis.editor.projects.delete"), CommonComponents.GUI_CANCEL));
    }

    /// Asks for a zip exported from the editor, and makes a project of it named after it
    private void importZip() {
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.zip"));
            filters.flip();
            path = TinyFileDialogs.tinyfd_openFileDialog(
                    Component.translatable("genesis.editor.projects.import.title").getString(),
                    null, filters, "ZIP", false);
        }
        if (path == null) return;
        Path zip = Path.of(path);
        String name = zip.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
        try {
            toSelect = projects.importZip(zip, name).folder();
            rebuildWidgets();
        } catch (IOException e) {
            failed("genesis.editor.projects.import.failed", e);
        }
    }

    private void openFolder() {
        try {
            Util.getPlatform().openPath(projects.root());
        } catch (IOException e) {
            failed("genesis.editor.projects.open_folder.failed", e);
        }
    }

    private void failed(String key, IOException e) {
        LOGGER.error("Celestial editor projects: {}", key, e);
        minecraft.getToasts().addToast(SystemToast.multiline(minecraft, TOAST, Component.translatable(key),
                Component.literal(String.valueOf(e.getMessage()))));
    }

    // List

    private final class ProjectList extends ObjectSelectionList<ProjectList.Entry> {
        ProjectList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        Entry add(Project project) {
            Entry entry = new Entry(project);
            addEntry(entry);
            return entry;
        }

        @Override
        public int getRowWidth() {
            return 270;
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            updateButtons();
        }

        private final class Entry extends ObjectSelectionList.Entry<Entry> {
            private final Project project;
            private long lastClick;

            Entry(Project project) {
                this.project = project;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(font, project.name(), left + 3, top + 1, 0xFFFFFF, false);
                String detail = project.fileName() + (project.lastSaved() > 0
                        ? " (" + DATE_FORMAT.format(new Date(project.lastSaved())) + ")" : "");
                guiGraphics.drawString(font, detail, left + 3, top + 1 + font.lineHeight + 3, 0x808080, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                setSelected(this);
                // Opened by a double click
                if (Util.getMillis() - lastClick < 250) {
                    open(project);
                    return true;
                }
                lastClick = Util.getMillis();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", project.name());
            }
        }
    }

    /// Asks for a project's name
    private static final class ProjectNameScreen extends Screen {
        private final Screen lastScreen;
        private final String initial;
        private final Consumer<String> onDone;
        private EditBox nameBox;
        private Button doneButton;

        ProjectNameScreen(Screen lastScreen, Component title, String initial, Consumer<String> onDone) {
            super(title);
            this.lastScreen = lastScreen;
            this.initial = initial;
            this.onDone = onDone;
        }

        @Override
        protected void init() {
            String value = nameBox == null ? initial : nameBox.getValue();
            nameBox = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20,
                    Component.translatable("genesis.editor.projects.name"));
            nameBox.setMaxLength(64);
            nameBox.setValue(value);
            nameBox.setResponder(text -> doneButton.active = !text.isBlank());
            addRenderableWidget(nameBox);
            doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
                    .bounds(width / 2 - 154, height / 2 + 20, 150, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                    .bounds(width / 2 + 4, height / 2 + 20, 150, 20).build());
            doneButton.active = !value.isBlank();
            setInitialFocus(nameBox);
        }

        private void done() {
            if (!nameBox.getValue().isBlank()) onDone.accept(nameBox.getValue().trim());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                done();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(lastScreen);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 44, 0xFFFFFF);
        }
    }
}
