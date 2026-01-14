#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform sampler2D Sampler0;

out vec4 frag_color;

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Interpolate between fog color and texture color based on alpha
    vec3 finalColor = mix(vertexColor.rgb, texColor.rgb, vertexColor.a);

    // Planets are always fully opaque
    frag_color = vec4(finalColor, 1.0);
}
