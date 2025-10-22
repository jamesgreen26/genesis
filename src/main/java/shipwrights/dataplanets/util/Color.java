package shipwrights.dataplanets.util;

public record Color(int red, int green, int blue, int alpha) {

    public Color {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
        alpha = clamp(alpha);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Create a Color from RGB values with full opacity (alpha = 255)
     */
    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    /**
     * Create a Color from a packed RGBA integer
     * Format: 0xRRGGBBAA
     */
    public static Color fromInt(int rgba) {
        int red = (rgba >> 24) & 0xFF;
        int green = (rgba >> 16) & 0xFF;
        int blue = (rgba >> 8) & 0xFF;
        int alpha = rgba & 0xFF;
        return new Color(red, green, blue, alpha);
    }

    /**
     * Convert this Color to a packed RGBA integer
     * Format: 0xRRGGBBAA
     */
    public int toInt() {
        return (red << 24) | (green << 16) | (blue << 8) | alpha;
    }

    /**
     * Create a new Color with modified alpha value
     */
    public Color withAlpha(int alpha) {
        return new Color(red, green, blue, alpha);
    }

    /**
     * Create a new Color with modified red value
     */
    public Color withRed(int red) {
        return new Color(red, green, blue, alpha);
    }

    /**
     * Create a new Color with modified green value
     */
    public Color withGreen(int green) {
        return new Color(red, green, blue, alpha);
    }

    /**
     * Create a new Color with modified blue value
     */
    public Color withBlue(int blue) {
        return new Color(red, green, blue, alpha);
    }

    /**
     * Convert to hex string format: #RRGGBBAA
     */
    public String toHexString() {
        return String.format("#%02X%02X%02X%02X", red, green, blue, alpha);
    }

    /**
     * Convert to hex string format without alpha: #RRGGBB
     */
    public String toHexStringNoAlpha() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    @Override
    public String toString() {
        return String.format("Color(r=%d, g=%d, b=%d, a=%d)", red, green, blue, alpha);
    }
}
