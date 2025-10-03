#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec2 texCoord0;
out vec4 vertexColor;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
}