#version 150

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(texCoord0, center);

    if (dist > 0.501) {
        discard;
    }

    float normalizedDist = dist / 0.5;

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, 1 - pow(normalizedDist, 0.125));
}