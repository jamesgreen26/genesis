#version 150

in vec2 texCoord;
in vec3 vertexColor;

uniform sampler2D Sampler0;

out vec4 frag_color;

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Apply lighting from vertex color
    vec3 finalColor = texColor.rgb * vertexColor;

    frag_color = vec4(finalColor, texColor.a);
}
