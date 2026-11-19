package shipwrights.genesis.space.client.map.editor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.MinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorPickerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
    }

    @Test
    void readsHueSaturationAndValue() {
        assertArrayEquals(new float[]{0, 1, 1}, ColorPicker.toHsv(0xFF0000), 1e-6f);
        assertArrayEquals(new float[]{1 / 3f, 1, 1}, ColorPicker.toHsv(0x00FF00), 1e-6f);
        assertArrayEquals(new float[]{2 / 3f, 1, 1}, ColorPicker.toHsv(0x0000FF), 1e-6f);
        assertArrayEquals(new float[]{0, 0, 0}, ColorPicker.toHsv(0x000000), 1e-6f);
        assertArrayEquals(new float[]{0, 0, 1}, ColorPicker.toHsv(0xFFFFFF), 1e-6f);
    }

    @Test
    void comesBackToTheSameColour() {
        for (int rgb : new int[]{0x05050A, 0x123456, 0xFFD040, 0x80FF80, 0xC0C0C0, 0x7F007F}) {
            float[] hsv = ColorPicker.toHsv(rgb);
            assertEquals(rgb, ColorPicker.fromHsv(hsv[0], hsv[1], hsv[2]), () -> Integer.toHexString(rgb));
        }
    }

    @Test
    void keepsItsHueThroughBlackAndGrey() {
        ColorPicker picker = new ColorPicker();
        picker.sync(0x2060C0);
        float hue = picker.hue();
        picker.sync(0x000000);
        assertEquals(hue, picker.hue());
        picker.sync(0x808080);
        assertEquals(hue, picker.hue());
        assertEquals(0x808080, picker.rgb());
    }
}
