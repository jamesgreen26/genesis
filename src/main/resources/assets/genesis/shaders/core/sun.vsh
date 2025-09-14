#version 150

in vec3 Position;
in vec4 Color;

out vec4 texCoord0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = Color;
}