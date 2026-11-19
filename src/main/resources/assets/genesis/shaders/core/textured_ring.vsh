#version 150

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;
out vec3 localPos;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
// From the celestial's local space, which the vertices are in, to where it's drawn
uniform mat4 LocalMat;

void main() {
    localPos = Position;
    gl_Position = ProjMat * ModelViewMat * LocalMat * vec4(Position, 1.0);
    texCoord = UV0;
}
