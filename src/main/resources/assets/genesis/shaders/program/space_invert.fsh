#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 invProjMat;
uniform mat4 invViewMat;
uniform vec3 cameraPos;

const vec3 LightPos = vec3(0.0);

in vec2 texCoord;
out vec4 fragColor;

vec3 reconstructPosition(vec2 uv, float z, mat4 InvVP) {
    float x = uv.x * 2.0 - 1.0;
    float y = (1.0 - uv.y) * 2.0 - 1.0;

    vec4 position_s = vec4(x, y, z, 1.0);
    vec4 position_v = InvVP * position_s;

    return position_v.xyz / position_v.w;
}


void main() {
    float depth = texture(MainDepthSampler, texCoord).r;

    vec3 P = reconstructPosition(texCoord, depth, invProjMat);
    vec3 normal = normalize(cross(dFdx(P), dFdy(P)));

    vec3 finalColor = normal * (1 - pow(depth, 32));
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
