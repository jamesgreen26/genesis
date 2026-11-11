package shipwrights.genesis.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.math.OBB;

/// Fades the screen out as the camera, in space, nears the surface of a celestial that can be visited: one with a
/// dimension of its own, sharing its id. Drawn over vanilla's freezing overlay, under the rest of the HUD.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CelestialApproachOverlay {

    /// How far from a celestial's surface, in blocks, the overlay becomes fully opaque
    private static final double OPAQUE_DISTANCE = 12.0;

    /// Over how many blocks, beyond {@link #OPAQUE_DISTANCE}, the overlay fades in
    private static final double FADE_DISTANCE = 12.0;

    /// RGB of the overlay
    private static final int COLOR = 0xC0C0C0;

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "celestial_approach", (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            float opacity = opacity(partialTick);
            if (opacity <= 0f) return;
            int alpha = Mth.clamp(Math.round(opacity * 255f), 0, 255);
            guiGraphics.fill(0, 0, screenWidth, screenHeight, alpha << 24 | COLOR);
        });
    }

    /// From 0 when no visitable celestial is near, to 1 within {@link #OPAQUE_DISTANCE} of a visitable celestial's surface
    private static float opacity(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ClientPacketListener connection = minecraft.getConnection();
        if (level == null || connection == null) return 0f;

        Time time = Time.get(level, partialTick);
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vector3d camera = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        if (!(VantagePoint.get(level, time, camera) instanceof VantagePoint.InSpace)) return 0f;

        Registry<Celestial> celestials = level.registryAccess().registry(Celestial.REGISTRY_KEY).orElse(null);
        if (celestials == null) return 0f;

        float opacity = 0f;
        for (var entry : celestials.entrySet()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, entry.getKey().location());
            if (!connection.levels().contains(dimension)) continue;

            Celestial celestial = entry.getValue();
            double distance = distanceToSurface(celestial, celestials, time, camera);
            float celestialOpacity = (float) Mth.clamp(1.0 - (distance - OPAQUE_DISTANCE) / FADE_DISTANCE, 0.0, 1.0);
            opacity = Math.max(opacity, celestialOpacity);
        }
        // Ease in, so the fade starts gently and only closes in right at the surface
        return opacity * opacity;
    }

    /// Distance from {@code point} to the surface of the cube {@code celestial} is drawn as; 0 inside it
    private static double distanceToSurface(Celestial celestial, Registry<Celestial> celestials, Time time, Vector3d point) {
        Celestial.TransformProvider transform = celestial.transformProvider();
        OBB cube = OBB.createCube(celestial.size(), transform.getRotation(celestials, time),
                transform.getPosition(celestials, time));
        return cube.closestPointTo(point).distance(point);
    }
}
