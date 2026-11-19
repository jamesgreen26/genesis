#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

out vec2 texCoord;
out vec3 lightDir;
out vec3 v_entry_position;
out vec3 surfacePos;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 LightDirection;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    v_entry_position = Normal;

    // Where on the cube's surface this is, for working out what shadow it's in
    surfacePos = Position;

    // UV0 contains the texture coordinates for the planet surface
    texCoord = UV0;
    lightDir = LightDirection;
}
