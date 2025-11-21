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

    vec4 pos_clip = vec4(texCoord*2-1, depth*2-1, 1.0);
    vec4 P_view = invProjMat * pos_clip;
    vec3 P_view3 = P_view.xyz / P_view.w;

    vec3 normalView = normalize(cross(dFdx(P_view3), dFdy(P_view3)));
    mat3 rotViewToWorld = mat3(invViewMat);
    vec3 normalWorld = normalize(rotViewToWorld * normalView);

    vec3 finalColor = abs(normalWorld);
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
