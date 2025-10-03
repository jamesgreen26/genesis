#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec3 positionData;
out float textureScale;
out vec3 vertexColor;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Unpack position data from UV
    // u contains r * 0.25 + g * 0.5, v contains b
    float r = step(0.25, mod(UV0.x, 0.5)); // Will be 1 if u is 0.25 or 0.75
    float g = step(0.5, UV0.x); // Will be 1 if u >= 0.5
    float b = UV0.y;

    positionData = vec3(r, g, b);
    textureScale = Color.a;
    vertexColor = Color.rgb;
}