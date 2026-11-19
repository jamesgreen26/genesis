package shipwrights.genesis.space.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.OrbitingTransformProvider;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.client.CelestialRenderDispatcher;
import shipwrights.genesis.space.client.CelestialVisualizer;
import shipwrights.genesis.space.client.Skybox;
import shipwrights.genesis.space.client.Starfield;
import shipwrights.genesis.space.client.map.editor.EditorPanel;
import shipwrights.genesis.space.client.map.editor.EditorProjects;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A map of the solar system, drawn with the celestials' own renderers. Drag to turn the view about the point in the
/// middle of the map, shift-drag to pan, and scroll to zoom. Time runs as the {@link TimeControls} in the top-left
/// say, and the view keeps up with the celestial it last flew to. F1 hides all but the celestials. The {@link EditorPanel} down the right makes new
/// celestials, which the map shows as they're made. Escape opens the {@link EditorMenuScreen}.
///
/// Only reachable from the title screen, by way of the {@link EditorProjectsScreen}, which picks the editor project to
/// work on: it reads the celestials from the default data packs itself, rather than from a world.
///
/// It starts out looking straight down, with north (-Z) at the top of the screen and east (+X) to the right. Positions
/// on the map are in blocks, as in space.
public class SolarSystemScreen extends Screen {
    private static final Component TITLE = Component.translatable("genesis.solar_system.title");

    /// How far the field of view may widen, at most. The map is meant to look flat, so it's kept narrow; it narrows
    /// further when the camera has to back off to stay clear of the tallest celestial.
    private static final double MAX_FOV = Math.toRadians(1.0);

    /// How far above the tallest celestial the camera stays at least, in multiples of its height, so that what sticks
    /// up out of the orbital plane isn't noticeably enlarged by perspective
    private static final double CLEARANCE = 20.0;

    private static final double MIN_BLOCKS_PER_PIXEL = 0.01;
    private static final double MAX_BLOCKS_PER_PIXEL = 1e5;
    private static final double ZOOM_STEP = 1.15;

    private static final int ORBIT_SEGMENTS = 256;
    private static final int ORBIT_COLOR = 0x40A0B4FF;

    /// Below this size on screen, in GUI pixels, a celestial gets a marker so it can be found
    private static final double MARKER_THRESHOLD = 6.0;

    /// Looking straight down, with north at the top of the screen
    // Not rotationX: in JOML 1.10.5, which Minecraft ships, Quaterniond.rotationX gives a wrong rotation
    private static final Quaterniondc TOP_DOWN = new Quaterniond().rotateX(Math.toRadians(90));
    /// How far the view turns for each GUI pixel the mouse is dragged
    private static final double ROTATE_SPEED = Math.toRadians(0.5);
    private static final Component RESET_CAMERA = Component.translatable("genesis.solar_system.reset_camera");
    /// How much of the map's frame a celestial flown to spans
    private static final double FOCUS_FILL = 0.35;

    private final Screen lastScreen;
    private final EditorProjects.Project project;

    /// The celestials in the game's own packs
    private OfflineCelestials builtIn;
    /// Those, with the editor's on top: what the map shows
    private Registry<Celestial> celestials;
    private Registry<CelestialVisualizer> visualizers;
    private boolean previewOutdated;
    private EditorPanel panel;
    /// The clock the map runs on, and its controls
    private final TimeControls timeControls = new TimeControls();
    /// The time on the map this frame
    private Time time = Time.of(0, 0f);

    /// Whether F1's hidden everything but the celestials: the widgets, the editor, labels and orbits. The map then
    /// takes the whole screen, and only it can be used.
    private boolean guiHidden;

    /// Whether the map, rather than a widget, was pressed on, and how far it's been dragged since
    private boolean mapPressed;
    private double mapDragged;
    /// Where on screen each celestial's label and marker were last drawn, to click them
    private final List<Target> targets = new ArrayList<>();

    /// The point the camera looks at, at the centre of the screen, in blocks
    private double centerX;
    private double centerY;
    private double centerZ;
    /// Blocks across one GUI pixel, at the distance of the point looked at
    private double blocksPerPixel = 1.0;
    /// The camera's view rotation, taking space into the camera's frame, where it looks along -Z with +Y up the screen
    private final Quaterniond rotation = new Quaterniond(TOP_DOWN);
    private boolean fitted = false;

    /// The celestial the editor's asked to be shown, to fly to once the map's caught up with the editor
    @Nullable
    private ResourceLocation flyToRequested;
    /// The view's way to a celestial, while it's on it
    @Nullable
    private CameraFlight flight;
    /// Which way the view's turned as the flight sets off, and as it gets there
    private final Quaterniond flightFromRotation = new Quaterniond();
    private final Quaterniond flightToRotation = new Quaterniond();
    /// The celestial the flight's to, if it's to one, and where it was as it set off. Followed more and more as the
    /// flight goes on, as it moves while time runs, then from then on.
    @Nullable
    private ResourceLocation flightTarget;
    private final Vector3d flightTargetStart = new Vector3d();

    /// The celestial the view keeps up with as time runs, since it was flown to, until the view's panned away
    @Nullable
    private ResourceLocation following;
    /// Where it was when last kept up with
    private final Vector3d followedAt = new Vector3d();

    /// @param lastScreen where to go back to once done
    /// @param project    the editor project to work on
    public SolarSystemScreen(Screen lastScreen, EditorProjects.Project project) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.project = project;
    }

    public EditorProjects.Project project() {
        return project;
    }

    public EditorPanel panel() {
        return panel;
    }

    @Override
    protected void init() {
        if (builtIn == null) {
            builtIn = OfflineCelestials.load();
            panel = new EditorPanel(this, builtIn, project, EditorProjectsScreen.exportsFolder(),
                    () -> previewOutdated = true, this::rebuildWidgets, id -> flyToRequested = id);
            updatePreview();
        }
        if (!fitted) {
            fitAll();
            fitted = true;
        }
        addRenderableWidget(new ItemIconButton(8, height - 28, 20, new ItemStack(Items.COMPASS), RESET_CAMERA,
                button -> resetCamera()));

        timeControls.init(this::addRenderableWidget);
        panel.init(width, height, this::addRenderableWidget);
    }

    /// Shows the editor's celestials on the map as they now are: on top of the game's own, replacing any with the same
    /// id. Those that can't be placed, such as for orbiting a celestial that isn't there, are left out.
    private void updatePreview() {
        previewOutdated = false;
        Map<ResourceLocation, Celestial> data = new LinkedHashMap<>();
        builtIn.registry().entrySet().forEach(entry -> data.put(entry.getKey().location(), entry.getValue()));
        data.putAll(panel.celestials());
        // Leave out any that can't be placed, rather than fail drawing them every frame
        celestials = OfflineCelestials.registryOf(OfflineCelestials.placeable(data, time));

        Map<ResourceLocation, CelestialVisualizer> looks = new LinkedHashMap<>();
        CelestialVisualizer.REGISTRY.registry().entrySet()
                .forEach(entry -> looks.put(entry.getKey().location(), entry.getValue()));
        looks.putAll(panel.appearances());
        MappedRegistry<CelestialVisualizer> lookRegistry = new MappedRegistry<>(CelestialVisualizer.REGISTRY_KEY, Lifecycle.stable());
        looks.forEach((id, look) -> lookRegistry.register(ResourceKey.create(CelestialVisualizer.REGISTRY_KEY, id), look,
                Lifecycle.stable()));
        lookRegistry.freeze();
        visualizers = lookRegistry;
    }

    @Override
    public void tick() {
        panel.tick();
    }

    @Override
    public void removed() {
        // Saved straight away, as this is the last word when the game's quit with the screen open
        panel.saveNow();
    }

    /// Flies back to looking straight down: at the celestial being edited, if the editor's open, or else at every
    /// celestial
    private void resetCamera() {
        Framing target = panel.isOpen() && panel.selected() != null ? framing(panel.selected()) : null;
        if (target == null) target = fullView();
        if (target != null) {
            flyTo(target, TOP_DOWN);
        } else {
            flight = null;
            rotation.set(TOP_DOWN);
        }
    }

    /// What the view's to show: the point in the middle of the map, beside the editor, and how far it's zoomed
    ///
    /// @param celestial the celestial it frames, to follow once there, if it frames one
    private record Framing(Vector3dc mapCenter, double blocksPerPixel, @Nullable ResourceLocation celestial) {}

    /// {@code id}, centred in the map at a size that sits well in it, or null if it's not on the map, as for one that
    /// can't be placed
    @Nullable
    private Framing framing(ResourceLocation id) {
        Celestial celestial = celestials.get(id);
        double frame = frameSize();
        if (celestial == null || frame <= 0) return null;
        Vector3dc position = celestial.transformProvider().getPosition(celestials, time);
        return new Framing(new Vector3d(position),
                Mth.clamp(celestial.size() / (frame * FOCUS_FILL), MIN_BLOCKS_PER_PIXEL, MAX_BLOCKS_PER_PIXEL), id);
    }

    /// Every celestial, looking straight down, leaving a margin, and room for the buttons along the bottom; as the view is
    /// centred, as much again at the top. Null if there's nothing to show.
    @Nullable
    private Framing fullView() {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Celestial celestial : celestials) {
            Vector3dc position = celestial.transformProvider().getPosition(celestials, time);
            double half = celestial.size() / 2;
            minX = Math.min(minX, position.x() - half);
            maxX = Math.max(maxX, position.x() + half);
            minZ = Math.min(minZ, position.z() - half);
            maxZ = Math.max(maxZ, position.z() + half);
        }
        if (minX > maxX) return null;

        double fit = Math.max((maxX - minX) / (mapRight() * 0.9), (maxZ - minZ) / ((height - 64) * 0.9));
        return new Framing(new Vector3d((minX + maxX) / 2, 0, (minZ + maxZ) / 2),
                Mth.clamp(fit, MIN_BLOCKS_PER_PIXEL, MAX_BLOCKS_PER_PIXEL), null);
    }

    /// Shows every celestial straight away, looking however the view already is
    private void fitAll() {
        Framing target = fullView();
        if (target == null) return;
        blocksPerPixel = target.blocksPerPixel();
        setMapCenter(target.mapCenter());
    }

    /// Sets off towards {@code target}, turning to {@code targetRotation} on the way
    private void flyTo(Framing target, Quaterniondc targetRotation) {
        double frame = frameSize();
        if (frame <= 0) return;
        // What's flown is the middle of the map, not of the screen, so that's what it zooms about
        Vector3d mapCenter = new Vector3d(centerX, centerY, centerZ).sub(mapCenterOffset(blocksPerPixel));
        flight = new CameraFlight(mapCenter, blocksPerPixel * frame,
                target.mapCenter(), target.blocksPerPixel() * frame, Util.getMillis());
        flightFromRotation.set(rotation);
        flightToRotation.set(targetRotation);
        following = null;
        flightTarget = target.celestial();
        flightTargetStart.set(target.mapCenter());
    }

    /// Moves the view along the flight it's on, if any
    private void stepFlight() {
        if (flight == null) return;
        double frame = frameSize();
        CameraFlight.View view = flight.at(Util.getMillis());
        // Turned first, as where the middle of the map is from the middle of the screen depends on it
        flightFromRotation.slerp(flightToRotation, view.progress(), rotation);
        if (view.done()) rotation.set(flightToRotation);
        if (frame > 0) blocksPerPixel = Mth.clamp(view.width() / frame, MIN_BLOCKS_PER_PIXEL, MAX_BLOCKS_PER_PIXEL);

        Vector3d center = view.center();
        Vector3d targetNow = flightTarget == null ? null : positionOf(flightTarget);
        if (targetNow != null) {
            // Where it was flown to has moved on since: caught up with bit by bit, so it's there at the end
            center.add(new Vector3d(targetNow).sub(flightTargetStart).mul(view.progress()));
        }
        setMapCenter(center);

        if (view.done()) {
            flight = null;
            following = targetNow == null ? null : flightTarget;
            if (targetNow != null) followedAt.set(targetNow);
        }
    }

    /// Keeps up with the celestial being followed, if any, moving the view as far as it's moved since last time.
    /// Only as far, so the view stays where it's been zoomed or turned to about it.
    private void follow() {
        if (flight != null || following == null) return;
        Vector3d now = positionOf(following);
        // Gone, as when deleted or renamed
        if (now == null) {
            following = null;
            return;
        }
        centerX += now.x - followedAt.x;
        centerY += now.y - followedAt.y;
        centerZ += now.z - followedAt.z;
        followedAt.set(now);
    }

    /// Where {@code id} is now, or null if it's not on the map
    @Nullable
    private Vector3d positionOf(ResourceLocation id) {
        Celestial celestial = celestials.get(id);
        return celestial == null ? null : new Vector3d(celestial.transformProvider().getPosition(celestials, time));
    }

    /// Looks at {@code point} in the middle of the map, beside the editor. The view's centred on the screen, so it's
    /// looked off to the right of it, by as far as the middle of the map is from the middle of the screen at the
    /// current zoom.
    private void setMapCenter(Vector3dc point) {
        Vector3d center = mapCenterOffset(blocksPerPixel).add(point);
        centerX = center.x;
        centerY = center.y;
        centerZ = center.z;
    }

    /// From the middle of the map, beside the editor, to the middle of the screen, in space, at
    /// {@code blocksPerPixel}
    private Vector3d mapCenterOffset(double blocksPerPixel) {
        double right = (width - mapRight()) / 2.0 * blocksPerPixel;
        return rotation.conjugate(new Quaterniond()).transform(new Vector3d(right, 0, 0));
    }

    /// The span of the map a celestial flown to is fitted in, in GUI pixels: the lesser of its width and height,
    /// leaving room for the buttons along the bottom as {@link #fitAll} does
    private double frameSize() {
        return Math.min(mapRight(), height - 64);
    }

    /// Saves, and goes back to where the map was opened from
    @Override
    public void onClose() {
        panel.close();
        minecraft.setScreen(lastScreen);
    }

    // Input

    private boolean onMap(double mouseX) {
        return mouseX < mapRight();
    }

    /// Where the map ends on the right: at the editor, if it's open and showing, or else at the screen's edge
    private int mapRight() {
        return guiHidden ? width : panel.left(width);
    }

    /// Hides or shows everything but the celestials, keeping what's in the middle of the map there, as the map
    /// widens or narrows
    private void toggleGui() {
        Vector3d mapCenter = new Vector3d(centerX, centerY, centerZ).sub(mapCenterOffset(blocksPerPixel));
        guiHidden = !guiHidden;
        if (guiHidden) {
            // Nothing's to be typed in or pressed unseen
            clearFocus();
            targets.clear();
        }
        setMapCenter(mapCenter);
    }

    /// Takes focus from whatever has it, telling each widget along the way
    private void clearFocus() {
        ComponentPath path = getCurrentFocusPath();
        if (path != null) path.applyFocus(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F1) {
            toggleGui();
            return true;
        }
        // Suggestions first: they take arrows, Tab, Enter and Escape from the box they're for
        if (!guiHidden && panel.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(new EditorMenuScreen(this));
            return true;
        }
        // Only Escape, while there's nothing else to use
        return !guiHidden && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return !guiHidden && super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (guiHidden) {
            if (button != 0) return false;
            mapPressed = true;
            mapDragged = 0;
            return true;
        }
        // Suggestions are drawn over the widgets, so get the click first
        if (button == 0 && panel.mouseClicked(mouseX, mouseY)) return true;
        unfocusFieldUnlessOver(mouseX, mouseY);
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || !onMap(mouseX)) return false;
        mapPressed = true;
        mapDragged = 0;
        return true;
    }

    /// Takes focus away from the text box being typed in, if any, unless it's what's clicked, so clicking anywhere
    /// else is done with it. Each container down to it is told too, as the form's rows don't pass it on themselves.
    private void unfocusFieldUnlessOver(double mouseX, double mouseY) {
        List<ContainerEventHandler> path = new ArrayList<>();
        GuiEventListener focused = this;
        while (focused instanceof ContainerEventHandler container && container.getFocused() != null) {
            path.add(container);
            focused = container.getFocused();
        }
        if (!(focused instanceof EditBox box) || box.isMouseOver(mouseX, mouseY)) return;
        box.setFocused(false);
        for (int i = path.size() - 1; i >= 0; i--) path.get(i).setFocused(null);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && panel.mouseReleased()) return true;
        if (mapPressed && button == 0) {
            mapPressed = false;
            // A click rather than a drag: select what was clicked, if it's one of the editor's
            if (mapDragged < 3 && onMap(mouseX)) {
                for (Target target : targets) {
                    if (target.contains(mouseX, mouseY) && panel.select(target.id())) break;
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && panel.mouseDragged(mouseX, mouseY)) return true;
        // Dragging a widget, as started by clicking one
        if (!mapPressed) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (button != 0) return false;
        // Taken over by hand
        flight = null;
        mapDragged += Math.abs(dragX) + Math.abs(dragY);
        if (!hasShiftDown()) {
            // As if grabbing the near side of a ball around the middle of the map, beside the editor: turned about
            // the screen's axes, so it follows the cursor whichever way the view is already turned
            Vector3d mapCenter = new Vector3d(centerX, centerY, centerZ).sub(mapCenterOffset(blocksPerPixel));
            rotation.premul(new Quaterniond()
                    .rotationY(dragX * ROTATE_SPEED)
                    .rotateX(dragY * ROTATE_SPEED))
                    .normalize();
            // The view's centred on the screen, which has swung round the middle of the map
            Vector3d center = mapCenter.add(mapCenterOffset(blocksPerPixel));
            centerX = center.x;
            centerY = center.y;
            centerZ = center.z;
        } else {
            // Across the screen, so what's under the cursor follows it. Away from whatever was followed.
            following = null;
            moveCenter(-dragX * blocksPerPixel, dragY * blocksPerPixel);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!guiHidden && panel.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (!guiHidden && super.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (scrollY == 0 || !onMap(mouseX)) return false;
        flight = null;
        // Keep the point under the cursor where it is
        double right = (mouseX - width / 2.0) * blocksPerPixel;
        double up = -(mouseY - height / 2.0) * blocksPerPixel;
        moveCenter(right, up);
        blocksPerPixel = Mth.clamp(blocksPerPixel * Math.pow(ZOOM_STEP, -scrollY), MIN_BLOCKS_PER_PIXEL, MAX_BLOCKS_PER_PIXEL);
        right = (mouseX - width / 2.0) * blocksPerPixel;
        up = -(mouseY - height / 2.0) * blocksPerPixel;
        moveCenter(-right, -up);
        return true;
    }

    /// Moves the point looked at across the screen, by {@code right} blocks towards its right and {@code up} towards
    /// its top
    private void moveCenter(double right, double up) {
        Quaterniond toSpace = rotation.conjugate(new Quaterniond());
        Vector3d move = toSpace.transform(new Vector3d(right, up, 0));
        centerX += move.x;
        centerY += move.y;
        centerZ += move.z;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (guiHidden) return;
        double mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        panel.onFilesDrop(paths, mouseX, mouseY);
    }

    // Rendering

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        if (guiHidden) return;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        panel.renderOverlay(guiGraphics);
    }

    /// Draws the map, behind the widgets: the celestials, their labels, and the editor's background
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0xFF000000);
        guiGraphics.flush();
        if (celestials == null) return;
        timeControls.advance();
        time = timeControls.time();
        if (previewOutdated) updatePreview();
        // Only now, so a celestial just made is on the map to be flown to
        if (flyToRequested != null) {
            // Turned however the view already is
            Framing target = framing(flyToRequested);
            if (target != null) flyTo(target, rotation);
            flyToRequested = null;
        }
        stepFlight();
        follow();

        renderMap();
        if (guiHidden) return;
        // Only over the map: not showing through the editor, nor clickable there
        guiGraphics.enableScissor(0, 0, panel.left(width), height);
        renderLabels(guiGraphics);
        guiGraphics.disableScissor();
        panel.render(guiGraphics);
    }

    private void renderMap() {
        Camera camera = camera();

        RenderSystem.backupProjectionMatrix();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(new Quaternionf(rotation));

        try {
            // The stars are a backdrop, seen with an ordinary field of view; through the map's they'd be enormous
            RenderSystem.setProjectionMatrix(new Matrix4f().setPerspective((float) Math.toRadians(70),
                    aspect(), 0.05f, 1000f), VertexSorting.DISTANCE_TO_ORIGIN);
            Skybox.render(poseStack, VantagePoint.InSpace.INSTANCE, 1f);
            Starfield.render(poseStack, VantagePoint.InSpace.INSTANCE, 1f);

            RenderSystem.setProjectionMatrix(new Matrix4f(camera.projection()), VertexSorting.DISTANCE_TO_ORIGIN);
            if (!guiHidden) renderOrbits(poseStack, camera.position());
            CelestialRenderDispatcher.renderCelestials(poseStack, camera.position(), celestials, visualizers, time,
                    VantagePoint.InSpace.INSTANCE, null, component -> true, 0, CelestialRenderDispatcher.DEPTH_STEP);
        } finally {
            GL11.glDepthRange(0, 1);
            GL11.glDisable(GL32.GL_DEPTH_CLAMP);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();

            RenderSystem.depthMask(true);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    /// Each celestial's orbit, around where its parent is now, drawn first so the celestials cover them
    private void renderOrbits(PoseStack poseStack, Vec3 cameraPosition) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        // Orbits pass far beyond the far plane when zoomed in
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);

        Matrix4f matrix = poseStack.last().pose();
        int a = ORBIT_COLOR >>> 24, r = ORBIT_COLOR >> 16 & 0xFF, g = ORBIT_COLOR >> 8 & 0xFF, b = ORBIT_COLOR & 0xFF;

        for (Celestial celestial : celestials) {
            if (!(celestial.transformProvider() instanceof OrbitingTransformProvider orbit)) continue;
            Celestial parent = celestials.get(orbit.parent());
            if (parent == null || orbit.period() == 0) continue;

            Vector3dc parentNow = parent.transformProvider().getPosition(celestials, time);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i <= ORBIT_SEGMENTS; i++) {
                double ticks = time.simple() + Math.abs(orbit.period()) * i / ORBIT_SEGMENTS;
                Time at = new Time((long) Math.floor(ticks), (float) (ticks - Math.floor(ticks)));
                Vector3dc position = orbit.getPosition(celestials, at);
                Vector3dc parentThen = parent.transformProvider().getPosition(celestials, at);
                builder.vertex(matrix,
                        (float) (position.x() - parentThen.x() + parentNow.x() - cameraPosition.x),
                        (float) (position.y() - parentThen.y() + parentNow.y() - cameraPosition.y),
                        (float) (position.z() - parentThen.z() + parentNow.z() - cameraPosition.z)
                ).color(r, g, b, a).endVertex();
            }
            BufferUploader.drawWithShader(builder.end());
        }

        GL11.glDisable(GL32.GL_DEPTH_CLAMP);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /// Marks the celestials too small to see, and names as many as fit without their names overlapping. Where names
    /// would overlap, the larger celestial's is shown. The one being edited is picked out.
    private void renderLabels(GuiGraphics guiGraphics) {
        Camera camera = camera();
        targets.clear();
        ResourceLocation selected = panel.isOpen() ? panel.selected() : null;
        List<Label> labels = new ArrayList<>();
        for (Map.Entry<ResourceKey<Celestial>, Celestial> entry : celestials.entrySet()) {
            Celestial celestial = entry.getValue();
            Vector3dc position = celestial.transformProvider().getPosition(celestials, time);

            // Through the same view and projection the celestials are drawn with
            Vector3d view = rotation.transform(new Vector3d(position).sub(
                    camera.position().x, camera.position().y, camera.position().z));
            Vector4d clip = camera.projection().transform(new Vector4d(view, 1));
            // Behind the camera
            if (clip.w <= 0) continue;
            double x = (clip.x / clip.w + 1) / 2 * width;
            double y = (1 - clip.y / clip.w) / 2 * height;
            // The projection's vertical scale is how many half screen heights one block across is, one block away
            double halfSize = celestial.size() / 2 * camera.projection().m11() / clip.w * height / 2;
            if (x + halfSize < 0 || x - halfSize > width || y + halfSize < 0 || y - halfSize > height) continue;

            int ix = (int) Math.round(x), iy = (int) Math.round(y);
            boolean isSelected = entry.getKey().location().equals(selected);
            int labelY;
            if (halfSize * 2 < MARKER_THRESHOLD) {
                guiGraphics.renderOutline(ix - 3, iy - 3, 7, 7, isSelected ? 0xFFFFD040 : 0xC0FFFFFF);
                labelY = iy + 6;
            } else {
                labelY = (int) Math.round(y + halfSize) + 3;
            }
            int reach = (int) Math.max(4, Math.min(halfSize, 4096));
            targets.add(new Target(ix - reach, iy - reach, ix + reach, iy + reach, entry.getKey().location()));

            Component name = name(entry.getKey().location());
            int labelWidth = font.width(name);
            labels.add(new Label(name, ix - labelWidth / 2, labelY, labelWidth, celestial.size(),
                    entry.getKey().location()));
        }

        labels.sort(Comparator.comparingDouble(Label::size).reversed().thenComparing(Label::id));
        List<Label> placed = new ArrayList<>();
        for (Label label : labels) {
            if (placed.stream().anyMatch(other -> other.overlaps(label, font.lineHeight))) continue;
            placed.add(label);
            guiGraphics.drawString(font, label.name(), label.x(), label.y(), label.id().equals(selected) ? 0xFFD040 : 0xFFFFFF);
            targets.add(new Target(label.x(), label.y(), label.x() + label.width(), label.y() + font.lineHeight, label.id()));
        }
    }

    /// Where a celestial was drawn on screen, to be clicked
    private record Target(int left, int top, int right, int bottom, ResourceLocation id) {
        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    /// A celestial's name, where it'd be drawn
    private record Label(Component name, int x, int y, int width, double size, ResourceLocation id) {
        boolean overlaps(Label other, int height) {
            return x < other.x + other.width && other.x < x + width && y < other.y + height && other.y < y + height;
        }
    }

    private static Component name(ResourceLocation id) {
        return Component.translatableWithFallback("celestial." + id.getNamespace() + "." + id.getPath(), id.toString());
    }

    private float aspect() {
        return (float) width / height;
    }

    /// Where the map is seen from: back from the point looked at, the way the camera's turned, far enough that what's
    /// around that point spans the screen at the current zoom, through a field of view no wider than {@link #MAX_FOV}
    private Camera camera() {
        double tallest = 0;
        for (Celestial celestial : celestials) {
            tallest = Math.max(tallest, Math.abs(celestial.transformProvider().getPosition(celestials, time).y())
                    + celestial.size() / 2);
        }
        double halfHeight = blocksPerPixel * height / 2;
        double distance = Math.max(halfHeight / Math.tan(MAX_FOV / 2), tallest * CLEARANCE);
        double fov = 2 * Math.atan(halfHeight / distance);

        // Backing off away from the way it's looking, which is -Z in its own frame
        Vector3d back = rotation.conjugate(new Quaterniond()).transform(new Vector3d(0, 0, distance));
        Vec3 position = new Vec3(centerX + back.x, centerY + back.y, centerZ + back.z);

        // Near and far planes don't matter much: celestials are drawn with depth clamping, at depths of their own
        Matrix4d projection = new Matrix4d().setPerspective(fov, aspect(), distance * 1e-3, distance * 4);
        return new Camera(position, projection);
    }

    /// @param projection the camera's projection, from its own frame (see {@link #rotation}) to clip space
    private record Camera(Vec3 position, Matrix4d projection) {}
}
