#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 InverseViewMatrix;
uniform vec3 CameraPos;

out vec3 vWorldPos;
out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out vec2 texCoord;
out vec3 vCameraPos;

void main()
{
    texCoord = UV0;
    vColor = Color;
    vNormal = Normal;

    vec4 world = ModelViewMat * vec4(Position, 1.0);
    vViewPos = InverseViewMatrix * Position;
    vWorldPos = vViewPos + CameraPos;
    vCameraPos = CameraPos;

    gl_Position = ProjMat * world;
}