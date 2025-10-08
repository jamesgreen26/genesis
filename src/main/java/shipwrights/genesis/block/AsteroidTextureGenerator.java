package shipwrights.genesis.block;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AsteroidTextureGenerator {

    public static void main(String[] args) {
        try {
            generate();
            System.out.println("Texture generation complete!");
        } catch (IOException e) {
            System.err.println("Error generating texture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generate() throws IOException {
        // Define rock/stone textures to use
        String[] rockTextures = {
            "src/main/resources/assets/genesis/textures/block/echostone.png",
            "src/main/resources/assets/genesis/textures/block/nullstone.png",
            "src/main/resources/assets/genesis/textures/block/phaserock.png",
            "src/main/resources/assets/genesis/textures/block/riftrock.png"
        };

        generate("asteroid", 16, rockTextures);
    }

    public static void generate(String baseName, int gridSize, String[] texturePaths) throws IOException {
        AsteroidTextureLayout layout = AsteroidTextureLayout.generate(gridSize, texturePaths.length);
        int[][] grid = layout.data;

        // Each texture tile is 16x16 pixels
        int tileSize = 16;
        int imageSize = gridSize * tileSize;

        // Create the output image
        BufferedImage outputImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);

        // Load source textures from the provided paths
        BufferedImage[] sourceTextures = new BufferedImage[texturePaths.length];
        for (int i = 0; i < texturePaths.length; i++) {
            File sourceFile = new File(texturePaths[i]);
            if (sourceFile.exists()) {
                sourceTextures[i] = ImageIO.read(sourceFile);
                System.out.println("Loaded texture: " + texturePaths[i]);
            } else {
                System.err.println("Warning: Source texture not found: " + texturePaths[i]);
                // Create a placeholder image
                sourceTextures[i] = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            }
        }

        // Copy tiles onto the output image based on the grid
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int textureId = grid[y][x];
                BufferedImage sourceTile = sourceTextures[textureId % sourceTextures.length];

                // Copy the tile to the output image
                for (int ty = 0; ty < tileSize; ty++) {
                    for (int tx = 0; tx < tileSize; tx++) {
                        int rgb = sourceTile.getRGB(tx, ty);
                        outputImage.setRGB(x * tileSize + tx, y * tileSize + ty, rgb);
                    }
                }
            }
        }

        // Save the output image
        String outputFilename = String.format("%s_combined.png", baseName);
        File outputFile = new File(outputFilename);
        ImageIO.write(outputImage, "png", outputFile);
        System.out.println("Generated texture: " + outputFilename + " (" + imageSize + "x" + imageSize + " pixels)");
    }
}
