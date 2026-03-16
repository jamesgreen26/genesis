package shipwrights.genesis_ad_astra;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Quick standalone test: creates a synthetic seed image (16×16 gas-giant-like stripes),
 * runs the full pipeline, and saves the six output faces as PNG files.
 *
 * Run from the project root:
 *   javac -d out planetgen/*.java
 *   java -cp out planetgen.PlanetGenTest
 */
public class PlanetGenTest {

    public static void main(String[] args) throws Exception {

        // ------------------------------------------------------------------
        // 1. Create or load a seed texture
        // ------------------------------------------------------------------
        BufferedImage seed;
        if (args.length > 0) {
            seed = ImageIO.read(new File(args[0]));
            System.out.println("Loaded seed texture: " + args[0]
                    + " (" + seed.getWidth() + "x" + seed.getHeight() + ")");
        } else {
            seed = syntheticGasGiant(16);
            System.out.println("Generated synthetic gas-giant seed (16×16).");
        }

        // ------------------------------------------------------------------
        // 2. Run the generator
        // ------------------------------------------------------------------
        PlanetTextureGenerator gen = new PlanetTextureGenerator()
                .resolution(256)
                .paletteSize(16)
                .dither(false)
                .seed(99887766L);

        BufferedImage[] faces = gen.generate(seed);

        // ------------------------------------------------------------------
        // 3. Save output
        // ------------------------------------------------------------------
        String[] faceNames = {"+X", "-X", "+Y", "-Y", "+Z", "-Z"};
        new File("output").mkdirs();
        for (int f = 0; f < 6; f++) {
            String filename = "output/face_" + f + "_" + faceNames[f].replace("+","pos").replace("-","neg") + ".png";
            ImageIO.write(faces[f], "PNG", new File(filename));
            System.out.println("Saved: " + filename);
        }

        // Also save a 3×2 strip overview for quick visual inspection
        BufferedImage strip = makeStrip(faces, 256);
        ImageIO.write(strip, "PNG", new File("output/planet_strip.png"));
        System.out.println("Saved overview: output/planet_strip.png");
    }

    // -----------------------------------------------------------------------
    // Synthetic seed images for testing
    // -----------------------------------------------------------------------

    /** Creates a 16×16 orange/brown gas-giant stripe pattern. */
    private static BufferedImage syntheticGasGiant(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[][] palette = {
            {210, 140,  60},  // warm orange
            {180, 100,  40},  // dark ochre
            {230, 170,  90},  // light amber
            {160,  80,  30},  // deep brown
        };
        for (int y = 0; y < size; y++) {
            int[] color = palette[(int)(Math.abs(Math.sin(y * Math.PI * 1.5)) * (palette.length - 1))];
            int r = color[0] + (int)(Math.random() * 15 - 7);
            int g = color[1] + (int)(Math.random() * 15 - 7);
            int b = color[2] + (int)(Math.random() * 10 - 5);
            for (int x = 0; x < size; x++) {
                img.setRGB(x, y, (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        }
        return img;
    }

    /** Creates a 3×2 horizontal strip of all six faces for easy inspection. */
    private static BufferedImage makeStrip(BufferedImage[] faces, int res) {
        BufferedImage strip = new BufferedImage(res * 3, res * 2, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = strip.createGraphics();
        // Layout:
        // Top row: North(-Z), West(-X), South(+Z)
        // Bottom row: East(+X), Down(-Y), Up(+Y)
        int[] order = {5, 1, 4, 0, 3, 2};
        for (int i = 0; i < order.length; i++) {
            int col = i % 3;
            int row = i / 3;
            g2d.drawImage(faces[order[i]], col * res, row * res, null);
        }
        g2d.dispose();
        return strip;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
