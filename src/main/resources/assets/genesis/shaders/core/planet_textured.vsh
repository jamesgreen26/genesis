#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out vec2 texCoord;
out vec4 vertexColor;
out vec3 vNormal;
out vec3 lightDir;
out vec3 v_entry_position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 LightDirection;
uniform float HalfSize;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    v_entry_position = Normal;

    // UV0 contains the texture coordinates for the planet surface
    texCoord = UV0;
    // Color.rgb contains fog color, Color.a contains alpha for interpolation
    vertexColor = Color;
    lightDir = LightDirection;

    // Pass through the per-vertex normal provided from Java
    vNormal = Normal;
}
