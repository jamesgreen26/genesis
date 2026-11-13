#version 150

#moj_import <genesis:dither.glsl>

in vec4 v_color;

uniform vec4 ColorModulator;
uniform float BaseAlpha;

out vec4 frag_color;

void main() {
    float t = clamp(v_color.a, 0.0, 1.0);
    float falloff = 1.0 - t;
    float alpha = BaseAlpha * falloff * falloff * falloff;
    vec3 color = v_color.rgb * ColorModulator.rgb;
    alpha *= ColorModulator.a;

    // Premultiplied, so the dithering isn't faded out along with the glow
    frag_color = vec4(dither(color * alpha), alpha);
}
