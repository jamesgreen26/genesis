#version 150

in vec3 Position;
in vec4 Color;

out vec3 v_camera_pos;
out vec3 v_entry_position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;

void main() {

    v_camera_pos = CameraPosition.xyz;
    v_entry_position = (Color.rgb * 1440) - 720;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}