#version 150

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // Output shadow color with alpha blending
    fragColor = vertexColor;
}
