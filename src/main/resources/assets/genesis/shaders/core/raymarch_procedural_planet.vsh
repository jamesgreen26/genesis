#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 InverseViewMatrix;
uniform vec3 CameraPos;
uniform int vPlanetIndex;

#moj_import <genesis:planetdata.glsl>

out vec3 vWorldPos;
out vec3 vModelPos;
out vec4 vColor;
out vec3 vNormal;
out vec3 vViewPos;
out vec3 vCameraPos;

void main()
{
    vColor = Color;
    vNormal = Normal;

    vec4 world = ModelViewMat * vec4(Position, 1.0);
    vViewPos = InverseViewMatrix * Position;
    vWorldPos = vViewPos + CameraPos;
    vModelPos = vWorldPos - planetPos[vPlanetIndex];
    vCameraPos = CameraPos;

    gl_Position = ProjMat * world;
}