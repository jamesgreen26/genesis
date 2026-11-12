package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/// Draws the outlines Create and its addons show on sub-levels, through Catnip's outliner, where they are, however
/// far the camera is from the level's origin.
///
/// An outline is drawn from where it is less where the camera is, as 32-bit floats. For one on a sub-level, Sable
/// first moves what's drawn by where the camera is in the level, so that taking the camera away again leaves it where
/// it is on the sub-level. Far out, those are two numbers too coarse to cancel: 60,000 blocks out they leave the
/// outline up to a two-hundredth of a block off, shaking as the camera moves, and ten million out, a block.
///
/// Any other position does as well as the camera's, so long as it's the same both times. So an outline that was on a
/// sub-level the last time it was drawn is drawn from where the camera is on that sub-level, which is near: a small
/// number, and the same one given to Sable to move what's drawn by. It's also what decides whether the camera is
/// inside the outline, which it now can.
///
/// Whether an outline is on a sub-level is only known once Sable has been asked to move it, by which time where the
/// camera is has been given. So it's remembered from the last time each was drawn, by the slot it's shown in: the
/// first time, or the first after it's left a sub-level, it's drawn as before.
///
/// Applied through {@link shipwrights.genesis.compat.catnip.mixin.OutlinerMixin} and
/// {@link shipwrights.genesis.compat.sable.mixin.SublevelRenderOffsetHelperMixin}. Only the client draws anything,
/// on its one thread
public final class OutlinePrecision {

    /// More slots than are ever shown at once: past this, what's remembered is forgotten, and found again
    private static final int MOST_REMEMBERED = 1024;

    /// The sub-level each slot's outline was on when last drawn, for those that were on one
    private static final Map<Object, WeakReference<SubLevel>> LAST_ON = new HashMap<>();

    /// The slot whose outline is being drawn, if one is
    private static Object drawing;

    /// Where it's being drawn from, in place of where the camera is, if anywhere
    private static Vec3 from;

    /// The sub-level Sable has moved it onto, if it has
    private static SubLevel on;

    private OutlinePrecision() {
    }

    /// The outline in {@code slot} is about to be drawn, for a camera at {@code camera}
    ///
    /// @return where it's to be drawn from
    public static Vec3 begin(Object slot, Vec3 camera) {
        drawing = slot;
        on = null;
        from = null;
        WeakReference<SubLevel> last = LAST_ON.get(slot);
        if (last != null && last.get() instanceof ClientSubLevel subLevel && !subLevel.isRemoved()) {
            // Where the camera is from the middle of the sub-level, as the sub-level is turned
            Pose3dc pose = subLevel.renderPose();
            Vector3d local = new Quaterniond(pose.orientation()).conjugate()
                    .transform(new Vector3d(camera.x, camera.y, camera.z).sub(pose.position()));
            from = new Vec3(local.x, local.y, local.z);
            return from;
        }
        return camera;
    }

    /// Sable is moving what's drawn onto {@code subLevel}
    ///
    /// @return what to move it by in place of where the camera is, or null to move it by that
    public static Vec3 movingOnto(SubLevel subLevel) {
        if (drawing == null) return null;
        on = subLevel;
        return from;
    }

    /// The outline that was {@linkplain #begin about to be drawn} has been
    public static void end() {
        if (drawing == null) return;
        if (on != null) {
            if (LAST_ON.size() >= MOST_REMEMBERED) LAST_ON.clear();
            LAST_ON.put(drawing, new WeakReference<>(on));
        } else {
            LAST_ON.remove(drawing);
        }
        drawing = null;
        from = null;
        on = null;
    }
}
