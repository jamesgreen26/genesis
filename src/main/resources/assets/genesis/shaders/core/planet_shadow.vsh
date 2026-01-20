#version 150

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out vec3 localPos;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    // Keep local-space position so the fragment shader can
    // compute distances relative to the cube vertices.
    localPos = Position;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
}
