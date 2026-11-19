package shipwrights.genesis.space.client.map;

import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/// A smooth pan and zoom from one view to another, along the path van Wijk and Nuij found seems quickest and easiest
/// to follow ("Smooth and efficient zooming and panning", 2003): for a far-off target it zooms out partway, so the
/// view never races across the screen, then back in. Close by, it's little more than a pan and a zoom together.
///
/// A view is the point looked at, and how wide a span around it is shown, in blocks.
final class CameraFlight {
    /// How much it'd rather zoom out than pan. Theirs, found to feel best.
    private static final double RHO = Math.sqrt(2);
    /// How long every flight takes, however far it goes or zooms
    private static final long DURATION_MS = 750;

    private final Vector3dc from;
    private final Vector3dc to;
    private final double fromWidth;
    private final double toWidth;
    /// How far the point looked at goes
    private final double distance;
    /// Whether that's too little to pan, so it only zooms
    private final boolean zoomOnly;
    private final double r0;
    /// The length of the path, in the paper's terms
    private final double length;
    private final long start;
    private final long durationMs;

    /// Where the view is partway, and whether it's got there
    ///
    /// @param progress how far through the flight it is, from 0 to 1, eased as the flight is, for whatever else is to
    ///                 change along with it
    record View(Vector3d center, double width, double progress, boolean done) {}

    /// @param now when it sets off, in milliseconds
    CameraFlight(Vector3dc from, double fromWidth, Vector3dc to, double toWidth, long now) {
        this.from = new Vector3d(from);
        this.to = new Vector3d(to);
        this.fromWidth = fromWidth;
        this.toWidth = toWidth;
        this.distance = from.distance(to);
        this.start = now;

        double rho2 = RHO * RHO;
        this.zoomOnly = distance < 1e-6 * Math.max(fromWidth, toWidth);
        if (zoomOnly) {
            r0 = 0;
            length = Math.abs(Math.log(toWidth / fromWidth)) / RHO;
        } else {
            double b0 = (toWidth * toWidth - fromWidth * fromWidth + rho2 * rho2 * distance * distance)
                    / (2 * fromWidth * rho2 * distance);
            double b1 = (toWidth * toWidth - fromWidth * fromWidth - rho2 * rho2 * distance * distance)
                    / (2 * toWidth * rho2 * distance);
            r0 = -asinh(b0);
            length = (-asinh(b1) - r0) / RHO;
        }
        // As long even with nowhere to go, as what else changes along with it, such as which way the view's turned,
        // still wants to change smoothly. At once only if the sums have failed, as for a width of nothing.
        this.durationMs = Double.isFinite(length) ? DURATION_MS : 0;
    }

    /// The view at {@code now}, in milliseconds. Eased in and out, so it sets off and settles gently.
    View at(long now) {
        double t = durationMs == 0 ? 1 : Mth.clamp((double) (now - start) / durationMs, 0, 1);
        // Exactly there at the end, whatever the sums come to
        if (t >= 1) return new View(new Vector3d(to), toWidth, 1, true);

        double eased = t * t * (3 - 2 * t);
        double s = eased * length;
        double width;
        double along;
        if (zoomOnly) {
            width = fromWidth * Math.exp(Math.signum(toWidth - fromWidth) * RHO * s);
            along = eased;
        } else {
            double rho2 = RHO * RHO;
            double u = fromWidth / rho2 * (Math.cosh(r0) * Math.tanh(RHO * s + r0) - Math.sinh(r0));
            width = fromWidth * Math.cosh(r0) / Math.cosh(RHO * s + r0);
            along = u / distance;
        }
        return new View(new Vector3d(from).lerp(to, along), width, eased, false);
    }

    /// The inverse hyperbolic sine, worked out so it doesn't lose precision for large values of either sign
    private static double asinh(double x) {
        double abs = Math.abs(x);
        return Math.copySign(Math.log(abs + Math.sqrt(abs * abs + 1)), x);
    }
}
