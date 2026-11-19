package shipwrights.genesis.space.client.map.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.GenesisMod;

/// A colour wheel, of hue around it and saturation out from the middle, with a slider beside it for brightness. Keeps
/// its own hue and saturation, so they aren't lost while the colour's black or grey, as they would be read back from
/// it.
final class ColorPicker {
    static final int WHEEL = 64;
    private static final int GAP = 6;
    private static final int SLIDER = 8;
    static final int WIDTH = WHEEL + GAP + SLIDER;
    static final int HEIGHT = WHEEL;
    /// Pixels across the wheel's texture: drawn at twice its size in the GUI, to stay sharp at the usual GUI scales
    private static final int TEXTURE_SIZE = WHEEL * 2;
    private static final ResourceLocation WHEEL_TEXTURE = GenesisMod.id("editor/color_wheel");

    private static boolean wheelMade;

    private float hue;
    private float saturation;
    private float value = 1;
    @Nullable
    private Part dragging;

    private enum Part {WHEEL, SLIDER}

    /// The colour it's on, as {@code 0xRRGGBB}
    int rgb() {
        return fromHsv(hue, saturation, value);
    }

    /// Moves onto {@code rgb}, if it's not on it already, keeping the hue and saturation where they can't be told
    /// from it
    void sync(int rgb) {
        if (rgb == rgb()) return;
        float[] hsv = toHsv(rgb);
        value = hsv[2];
        if (value > 0) {
            saturation = hsv[1];
            if (saturation > 0) hue = hsv[0];
        }
    }

    /// The hue it's on, from 0 to 1, which it keeps even while the colour's black or grey
    float hue() {
        return hue;
    }

    boolean isDragging() {
        return dragging != null;
    }

    /// Starts picking, if pressed on the wheel or slider, drawn at {@code left}, {@code top}
    ///
    /// @return whether it was pressed on
    boolean press(double mouseX, double mouseY, int left, int top) {
        double dx = mouseX - (left + WHEEL / 2.0), dy = mouseY - (top + WHEEL / 2.0);
        if (dx * dx + dy * dy <= (WHEEL / 2.0 + 2) * (WHEEL / 2.0 + 2)) {
            dragging = Part.WHEEL;
        } else if (mouseX >= left + WHEEL + GAP - 2 && mouseX < left + WIDTH + 2 && mouseY >= top - 2 && mouseY < top + HEIGHT + 2) {
            dragging = Part.SLIDER;
        } else {
            return false;
        }
        drag(mouseX, mouseY, left, top);
        return true;
    }

    /// Picks where the mouse is, on whichever it was pressed on, even once it's off it
    void drag(double mouseX, double mouseY, int left, int top) {
        if (dragging == Part.WHEEL) {
            double dx = mouseX - (left + WHEEL / 2.0), dy = mouseY - (top + WHEEL / 2.0);
            double radius = Math.sqrt(dx * dx + dy * dy) / (WHEEL / 2.0);
            saturation = (float) Math.min(radius, 1);
            if (radius > 0) hue = (float) Mth.positiveModulo(Math.atan2(dy, dx) / (2 * Math.PI), 1.0);
        } else if (dragging == Part.SLIDER) {
            value = (float) Mth.clamp(1 - (mouseY - top) / HEIGHT, 0, 1);
        }
    }

    void release() {
        dragging = null;
    }

    void render(GuiGraphics guiGraphics, int left, int top) {
        // The wheel, darkened to the brightness, as a colour's brightness scales each of its channels
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(value, value, value, 1);
        guiGraphics.blit(wheelTexture(), left, top, WHEEL, WHEEL, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        guiGraphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        double angle = hue * 2 * Math.PI;
        int markX = left + WHEEL / 2 + (int) Math.round(Math.cos(angle) * saturation * WHEEL / 2);
        int markY = top + WHEEL / 2 + (int) Math.round(Math.sin(angle) * saturation * WHEEL / 2);
        marker(guiGraphics, markX - 2, markY - 2, 5, 5);

        int sliderLeft = left + WHEEL + GAP;
        guiGraphics.fillGradient(sliderLeft, top, sliderLeft + SLIDER, top + HEIGHT,
                0xFF000000 | fromHsv(hue, saturation, 1), 0xFF000000);
        guiGraphics.renderOutline(sliderLeft - 1, top - 1, SLIDER + 2, HEIGHT + 2, 0xFF505064);
        int sliderY = top + Math.round((1 - value) * (HEIGHT - 1));
        marker(guiGraphics, sliderLeft - 2, sliderY - 1, SLIDER + 4, 3);
    }

    /// A box outlined in white and black, to show up on any colour
    private static void marker(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFF000000);
        guiGraphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
    }

    /// Every hue around and saturation out from the middle at full brightness, made the first time it's drawn
    private static ResourceLocation wheelTexture() {
        if (wheelMade) return WHEEL_TEXTURE;
        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        double half = TEXTURE_SIZE / 2.0;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                double dx = (x + 0.5 - half) / half, dy = (y + 0.5 - half) / half;
                double radius = Math.sqrt(dx * dx + dy * dy);
                // Faded out over its last pixel, so its edge is smooth
                int alpha = (int) Math.round(Mth.clamp((1 - radius) * half + 0.5, 0, 1) * 255);
                float hue = (float) Mth.positiveModulo(Math.atan2(dy, dx) / (2 * Math.PI), 1.0);
                int rgb = fromHsv(hue, (float) Math.min(radius, 1), 1);
                // As NativeImage has them: alpha, blue, green, red
                int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
                image.setPixelRGBA(x, y, alpha << 24 | b << 16 | g << 8 | r);
            }
        }
        Minecraft.getInstance().getTextureManager().register(WHEEL_TEXTURE, new DynamicTexture(image));
        wheelMade = true;
        return WHEEL_TEXTURE;
    }

    /// Hue, saturation and value, each from 0 to 1, as {@code 0xRRGGBB}. Not {@link Mth#hsvToRgb}, which rounds each
    /// channel down, so colours typed in wouldn't come back the same.
    static int fromHsv(float hue, float saturation, float value) {
        float h = (float) Mth.positiveModulo(hue, 1.0) * 6;
        int sector = (int) h % 6;
        float f = h - (int) h;
        float p = value * (1 - saturation), q = value * (1 - f * saturation), t = value * (1 - (1 - f) * saturation);
        float r, g, b;
        switch (sector) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    /// {@code 0xRRGGBB} as hue, saturation and value, each from 0 to 1
    static float[] toHsv(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0) hue = 0;
        else if (max == r) hue = ((g - b) / delta) / 6;
        else if (max == g) hue = ((b - r) / delta + 2) / 6;
        else hue = ((r - g) / delta + 4) / 6;
        if (hue < 0) hue += 1;
        return new float[]{hue, max == 0 ? 0 : delta / max, max};
    }
}
