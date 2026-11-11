package shipwrights.genesis.space.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;

/// Fades the screen out as the camera nears where travel between space and a celestial's dimension happens: in space,
/// the surface of a celestial that can be visited, one with a dimension of its own sharing its id; in a celestial's
/// dimension, the {@linkplain GenesisCommonConfig#atmosphereExitHeight() exit height}. Drawn over vanilla's freezing
/// overlay, under the rest of the HUD.
///
/// Below the exit height, the camera is taken to be as far from it as from the distance in space at which things
/// arrive at a celestial, {@link CelestialTravel#ARRIVAL_DISTANCE}, so the screen fades the same way approaching either.
///
/// When travelling, the overlay is fully opaque through the change of dimension, which shows no loading screens, and
/// until the new dimension has loaded around the camera, then fades out over {@link #ARRIVAL_FADE_TICKS}, whatever the
/// camera's position. The more opaque of that and the overlay for the camera's position shows.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CelestialApproachOverlay {

    /// How far from a celestial's surface, in blocks, the overlay becomes fully opaque
    private static final double OPAQUE_DISTANCE = 12.0;

    /// Over how many blocks, beyond {@link #OPAQUE_DISTANCE}, the overlay fades in
    private static final double FADE_DISTANCE = 12.0;

    /// Over how many ticks after travelling the overlay fades out
    private static final int ARRIVAL_FADE_TICKS = 40;

    /// RGB of the overlay
    private static final int COLOR = 0xC0C0C0;

    /// Ticks left of the fade after travelling
    private static int arrivalFadeTicks = 0;

    /// How long to wait for the change of dimension after being told to expect it, in case it never comes
    private static final int TRAVEL_TIMEOUT_TICKS = 100;

    /// Ticks left to wait for the change of dimension, while it's expected
    private static int travelTimeoutTicks = 0;

    /// Called when this client's player is about to travel between space and a celestial's dimension, before the change
    /// of dimension reaches it
    public static void startTravel() {
        arrivalFadeTicks = ARRIVAL_FADE_TICKS;
        travelTimeoutTicks = TRAVEL_TIMEOUT_TICKS;
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "celestial_approach", (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            float opacity = opacity(partialTick);
            if (opacity <= 0f) return;
            int alpha = Mth.clamp(Math.round(opacity * 255f), 0, 255);
            guiGraphics.fill(0, 0, screenWidth, screenHeight, alpha << 24 | COLOR);
        });
    }

    private static float opacity(float partialTick) {
        float positional = positionalOpacity(partialTick);
        float arrival = Mth.clamp((arrivalFadeTicks - partialTick) / ARRIVAL_FADE_TICKS, 0f, 1f);
        // Ease out, so it clears gently at the end
        return Math.max(positional, arrival * arrival);
    }

    /// From 0 far from where travel happens, to 1 within {@link #OPAQUE_DISTANCE} of it
    private static float positionalOpacity(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ClientPacketListener connection = minecraft.getConnection();
        if (level == null || connection == null) return 0f;

        Time time = Time.get(level, partialTick);
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vector3d camera = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);

        VantagePoint vantagePoint = VantagePoint.get(level, time, camera);
        double distance;
        if (vantagePoint instanceof VantagePoint.InSpace) {
            distance = distanceToVisitableCelestial(level, connection, time, camera);
        } else if (vantagePoint instanceof VantagePoint.OnCelestial) {
            distance = distanceToExitHeight(connection, camera.y);
        } else {
            return 0f;
        }
        float opacity = (float) Mth.clamp(1.0 - (distance - OPAQUE_DISTANCE) / FADE_DISTANCE, 0.0, 1.0);
        // Ease in, so the fade starts gently and only closes in right at the surface
        return opacity * opacity;
    }

    /// From the surface of the nearest celestial with a dimension of its own
    private static double distanceToVisitableCelestial(ClientLevel level, ClientPacketListener connection, Time time,
                                                       Vector3d camera) {
        Registry<Celestial> celestials = level.registryAccess().registry(Celestial.REGISTRY_KEY).orElse(null);
        if (celestials == null) return Double.POSITIVE_INFINITY;

        double nearest = Double.POSITIVE_INFINITY;
        for (var entry : celestials.entrySet()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, entry.getKey().location());
            if (!connection.levels().contains(dimension)) continue;
            nearest = Math.min(nearest, distanceToSurface(entry.getValue(), celestials, time, camera));
        }
        return nearest;
    }

    /// The exit height, as if it were {@link CelestialTravel#ARRIVAL_DISTANCE} from a celestial's surface in space.
    /// Uses this client's copy of the common config, which the server doesn't send it
    private static double distanceToExitHeight(ClientPacketListener connection, double cameraY) {
        if (!connection.levels().contains(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE))) {
            return Double.POSITIVE_INFINITY;
        }
        return GenesisCommonConfig.atmosphereExitHeight() - cameraY + CelestialTravel.ARRIVAL_DISTANCE;
    }

    /// Distance from {@code point} to the surface of the cube {@code celestial} is drawn as; 0 inside it
    private static double distanceToSurface(Celestial celestial, Registry<Celestial> celestials, Time time, Vector3d point) {
        Celestial.TransformProvider transform = celestial.transformProvider();
        OBB cube = OBB.createCube(celestial.size(), transform.getRotation(celestials, time),
                transform.getPosition(celestials, time));
        return cube.closestPointTo(point).distance(point);
    }

    @Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
    public static class ArrivalFade {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (travelTimeoutTicks > 0) {
                travelTimeoutTicks--;
                return;
            }
            // Not while the new dimension is still loading, behind its (invisible) loading screen
            if (arrivalFadeTicks > 0 && !(Minecraft.getInstance().screen instanceof ReceivingLevelScreen)) {
                arrivalFadeTicks--;
            }
        }

        /// Vanilla shows a progress screen as it changes the dimension, then a loading screen until the new one has
        /// loaded around the camera. Show neither, so the overlay hides the change, but keep the loading screen's
        /// waiting for the dimension to load, and keeping the player still meanwhile
        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (travelTimeoutTicks <= 0) return;
            Screen screen = event.getNewScreen();
            if (screen instanceof ProgressScreen) {
                event.setNewScreen(new InvisibleScreen());
            } else if (screen instanceof ReceivingLevelScreen && !(screen instanceof InvisibleLoadingScreen)) {
                event.setNewScreen(new InvisibleLoadingScreen());
                travelTimeoutTicks = 0;
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            arrivalFadeTicks = 0;
            travelTimeoutTicks = 0;
        }
    }

    /// A screen that opening doesn't free the mouse or let go of held keys for (see
    /// {@link shipwrights.genesis.mixin.MinecraftMixin}), so the camera keeps turning and what's held keeps happening
    /// while it's open. Keys let go of while it's open don't reach their bindings, so they're brought up to date as it
    /// closes.
    public interface KeepsInput {
    }

    private static final class InvisibleScreen extends Screen implements KeepsInput {
        InvisibleScreen() {
            super(CommonComponents.EMPTY);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public void removed() {
            KeyMapping.setAll();
        }
    }

    private static final class InvisibleLoadingScreen extends ReceivingLevelScreen implements KeepsInput {
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void removed() {
            KeyMapping.setAll();
        }
    }
}
