package shipwrights.genesis.space.client.map.editor;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TexturePreviewsTest {

    @Test
    void readsPngsLargerThanTheMemoryStack() throws IOException {
        // Noise doesn't compress, so this comes to several times LWJGL's 64 KiB stack
        BufferedImage image = new BufferedImage(384, 256, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(1);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, random.nextInt());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] png = out.toByteArray();
        assertTrue(png.length > 256 * 1024, () -> "Only " + png.length + " bytes");

        try (NativeImage read = TexturePreviews.readPng(png)) {
            assertEquals(384, read.getWidth());
            assertEquals(256, read.getHeight());
        }
    }
}
