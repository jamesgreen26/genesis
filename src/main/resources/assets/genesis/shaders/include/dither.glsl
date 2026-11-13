// Dithering, to hide the colour banding that smooth gradients show once rounded to 8 bits per channel.
//
// The noise has to reach the framebuffer at full strength, so translucent shaders that use it output premultiplied
// alpha (see PremultipliedTransparency): were the blending to multiply the colour by alpha afterwards, the noise would
// fade out with it, right where the gradients are faintest and band the most.

// Interleaved gradient noise (Jimenez 2014), from 0 to 1: evenly spread across the screen, with no visible clumps
float ditherPattern() {
    return fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
}

// Up to half an 8-bit step either way, so that rounding the result averages out to the colour itself. Where the colour
// is under half a step from black the noise would take it below zero, which is clamped, but rounds to zero regardless.
vec3 dither(vec3 color) {
    return color + (ditherPattern() - 0.5) / 255.0;
}

// As dither(), for colours that darken what's behind them and may be black themselves, such as shadows. The noise
// then has nowhere to go but up: it's a whole step upwards instead, making the result half a step brighter.
vec3 ditherUpwards(vec3 color) {
    return color + ditherPattern() / 255.0;
}
