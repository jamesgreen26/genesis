#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform float GameTime;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    // Derive rotation axis from color (normalize to get a unit vector)
    vec3 axis = normalize(Color.rgb * 2.0 - 1.0);

    // Rotation angle based on GameTime
    float angle = GameTime * 3.14159265359 * 24;

    // Compute rotation matrix using Rodrigues' rotation formula
    float c = cos(angle);
    float s = sin(angle);
    float t = 1.0 - c;

    mat3 rotationMatrix = mat3(
        t * axis.x * axis.x + c,          t * axis.x * axis.y - s * axis.z, t * axis.x * axis.z + s * axis.y,
        t * axis.x * axis.y + s * axis.z, t * axis.y * axis.y + c,          t * axis.y * axis.z - s * axis.x,
        t * axis.x * axis.z - s * axis.y, t * axis.y * axis.z + s * axis.x, t * axis.z * axis.z + c
    );

    // Apply rotation to position
    vec3 rotatedPosition = rotationMatrix * Position;

    gl_Position = ProjMat * ModelViewMat * vec4(rotatedPosition, 1.0);

    texCoord0 = UV0;
    vertexColor = vec4(min(Color.r, min(Color.g, Color.b)), Color.gba);
}