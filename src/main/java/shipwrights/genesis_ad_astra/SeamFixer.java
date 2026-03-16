package shipwrights.genesis_ad_astra;

import java.awt.image.BufferedImage;

/**
 * Fixes seams between adjacent cube faces by blending a thin border region.
 *
 * Because the TextureSynthesizer already samples a shared 3D noise field,
 * seams should be minimal; this pass performs a light 2–4 pixel edge blend
 * as a final safety net.
 *
 * Adjacency table (OpenGL face convention):
 *   face 0 (+X): right=5, left=4, top=2, bottom=3
 *   face 1 (-X): right=4, left=5, top=2, bottom=3
 *   face 2 (+Y): right=0, left=1, top=5, bottom=4
 *   face 3 (-Y): right=0, left=1, top=4, bottom=5
 *   face 4 (+Z): right=0, left=1, top=2, bottom=3
 *   face 5 (-Z): right=1, left=0, top=2, bottom=3
 */
public class SeamFixer {

    private static final int BLEND_WIDTH = 3;

    /**
     * Apply seam blending in-place on the six cube faces.
     */
    public void fix(BufferedImage[] faces) {
        int res = faces[0].getWidth();

        // Blend every pair of adjacent edges
        // Format: {faceA, edgeA, faceB, edgeB, reverseB}
        // edge: 0=top, 1=bottom, 2=left, 3=right
        int[][] adjacency = {
            {0, 2, 4, 3, false ? 1 : 0},  // face0 left  ↔ face4 right
            {0, 3, 5, 2, 0},               // face0 right ↔ face5 left
            {0, 0, 2, 3, 0},               // face0 top   ↔ face2 right
            {0, 1, 3, 3, 0},               // face0 bottom↔ face3 right
            {1, 2, 5, 3, 0},               // face1 left  ↔ face5 right
            {1, 3, 4, 2, 0},               // face1 right ↔ face4 left
            {1, 0, 2, 2, 0},               // face1 top   ↔ face2 left
            {1, 1, 3, 2, 0},               // face1 bottom↔ face3 left
            {4, 0, 2, 1, 0},               // face4 top   ↔ face2 bottom
            {4, 1, 3, 0, 0},               // face4 bottom↔ face3 top
            {5, 0, 2, 0, 0},               // face5 top   ↔ face2 top  (reversed)
            {5, 1, 3, 1, 0},               // face5 bottom↔ face3 bottom
        };

        for (int[] adj : adjacency) {
            blendEdge(faces, adj[0], adj[1], adj[2], adj[3], res);
        }
    }

    // -------------------------------------------------------------------------
    // Edge blend
    // -------------------------------------------------------------------------

    /**
     * Blend the border pixels between edgeA of faceA and edgeB of faceB.
     * edge indices: 0=top, 1=bottom, 2=left, 3=right
     */
    private void blendEdge(BufferedImage[] faces, int fA, int eA, int fB, int eB, int res) {
        for (int i = 0; i < res; i++) {
            for (int w = 0; w < BLEND_WIDTH; w++) {
                float t = (float) w / BLEND_WIDTH; // 0 = edge, 1 = inner

                int[] pA = getEdgePixel(faces[fA], eA, i, w, res);
                int[] pB = getEdgePixel(faces[fB], eB, i, w, res);

                int[] blendA = blend(pA, pB, t);
                int[] blendB = blend(pB, pA, t);

                setEdgePixel(faces[fA], eA, i, w, res, blendA);
                setEdgePixel(faces[fB], eB, i, w, res, blendB);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Edge pixel accessors
    // -------------------------------------------------------------------------

    private int[] getEdgePixel(BufferedImage img, int edge, int i, int depth, int res) {
        int x = 0, y = 0;
        switch (edge) {
            case 0: x = i;           y = depth;         break; // top
            case 1: x = i;           y = res-1-depth;   break; // bottom
            case 2: x = depth;       y = i;             break; // left
            case 3: x = res-1-depth; y = i;             break; // right
        }
        int rgb = img.getRGB(clamp(x, res), clamp(y, res));
        return new int[]{(rgb>>16)&0xFF, (rgb>>8)&0xFF, rgb&0xFF};
    }

    private void setEdgePixel(BufferedImage img, int edge, int i, int depth, int res, int[] color) {
        int x = 0, y = 0;
        switch (edge) {
            case 0: x = i;           y = depth;         break;
            case 1: x = i;           y = res-1-depth;   break;
            case 2: x = depth;       y = i;             break;
            case 3: x = res-1-depth; y = i;             break;
        }
        img.setRGB(clamp(x, res), clamp(y, res),
                (color[0] << 16) | (color[1] << 8) | color[2]);
    }

    private int[] blend(int[] a, int[] b, float t) {
        return new int[]{
            clampV(Math.round(a[0] + t * (b[0] - a[0]))),
            clampV(Math.round(a[1] + t * (b[1] - a[1]))),
            clampV(Math.round(a[2] + t * (b[2] - a[2])))
        };
    }

    private int clamp(int v, int max) { return Math.max(0, Math.min(max-1, v)); }
    private int clampV(int v)         { return Math.max(0, Math.min(255, v)); }
}
