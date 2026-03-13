#version 150

in vec3 Position;
in vec4 Color;

out vec3 v_camera_pos;
out vec3 v_entry_position;
out float v_half_size;
out vec3 lightDir;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;
uniform float HalfSize;
uniform vec3 LightDirection;
uniform float AtmosphereThickness;

void main() {

    v_camera_pos = CameraPosition.xyz;
    v_entry_position = ((Color.rgb * HalfSize * 2) - HalfSize) * AtmosphereThickness;
    v_half_size = HalfSize;

    lightDir = LightDirection;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}