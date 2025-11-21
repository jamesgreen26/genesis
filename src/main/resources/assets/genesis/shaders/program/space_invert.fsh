#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 invProjMat;
uniform mat4 invViewMat;
uniform vec3 cameraPos;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float depth = texture(MainDepthSampler, texCoord).r;
    if (depth < 1.0) {

        vec4 pos_clip = vec4(texCoord * 2 - 1, depth * 2 - 1, 1.0);
        vec4 P_view = invProjMat * pos_clip;
        vec3 P_view3 = P_view.xyz / P_view.w;

        vec3 normalView = normalize(cross(dFdx(P_view3), dFdy(P_view3)));
        mat3 rotViewToWorld = mat3(invViewMat);
        vec3 normalWorld = normalize(rotViewToWorld * normalView);

        vec3 P_world = (invViewMat * vec4(P_view3, 1.0)).xyz;

        vec3 light_vec = normalize(P_world + cameraPos);

        float brightness = clamp(0.5 - dot(light_vec, normalWorld), 0, 2);

        fragColor = texture(DiffuseSampler, texCoord) * vec4(brightness, brightness, brightness, 1.0);
    } else {
        fragColor = texture(DiffuseSampler, texCoord);
    }
}
