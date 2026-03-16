package shipwrights.genesis_ad_astra;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

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
        File inputsDir = new File("src/main/java/shipwrights/genesis_ad_astra/inputs");
        File[] inputFiles = inputsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });
        if (inputFiles == null || inputFiles.length == 0) {
            System.out.println("No seed images found in " + inputsDir.getPath() + " (png/jpg/jpeg).");
            return;
        }
        Arrays.sort(inputFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // ------------------------------------------------------------------
        // 2. Run the generator
        // ------------------------------------------------------------------
        PlanetTextureGenerator gen = new PlanetTextureGenerator()
                .resolution(256)
                .paletteSize(16)
                .dither(true)
                .seed(99887766L);

        // ------------------------------------------------------------------
        // 3. Process each input and save output
        // ------------------------------------------------------------------
        String[] faceNames = {"+X", "-X", "+Y", "-Y", "+Z", "-Z"};
        new File("output").mkdirs();
        for (File inputFile : inputFiles) {
            BufferedImage seedImage = ImageIO.read(inputFile);
            System.out.println("Loaded seed texture: " + inputFile.getPath()
                    + " (" + seedImage.getWidth() + "x" + seedImage.getHeight() + ")");

            String baseName = stripExtension(inputFile.getName());
            BufferedImage[] faces = gen.generate(seedImage);


            for (int f = 0; f < 6; f++) {
                String filename = "output/face_" + f + "_" + faceNames[f].replace("+","pos").replace("-","neg")
                        + "_" + baseName + ".png";
                ImageIO.write(faces[f], "PNG", new File(filename));
                System.out.println("Saved: " + filename);
            }

            BufferedImage strip = makeStrip(faces, 256);
            String stripName = "output/planet_strip_" + baseName + ".png";
            ImageIO.write(strip, "PNG", new File(stripName));
            System.out.println("Saved overview: " + stripName);
        }
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

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
