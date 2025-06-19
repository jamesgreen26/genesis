in vec3 position;
in vec2 UV0;

out vec2 texCoord0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    texCoord0 = UV0;
}