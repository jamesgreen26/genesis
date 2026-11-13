#version 150

#moj_import <genesis:dither.glsl>

// Vanilla's vignette texture: how much to darken the screen by
uniform sampler2D Sampler0;

// Its strength, the same in every channel
uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float darkening = texture(Sampler0, texCoord0).r * ColorModulator.r;

    // The screen is scaled by one minus the alpha, and the colour is added to it: only the dithering. The vignette
    // darkens, so the noise can only go upwards (see ditherUpwards() in dither.glsl). It fades in over the first 8-bit
    // step of darkening, leaving the middle of the screen, which the vignette doesn't reach, exactly as it was.
    float noise = ditherPattern() / 255.0 * min(darkening * 255.0, 1.0);

    fragColor = vec4(vec3(noise), darkening);
}
