#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D PlanetMaskSampler;
uniform sampler2D PlanetDepthSampler;

uniform mat4 invProjMat;
uniform mat4 invViewMat;
uniform vec3 cameraPos;
uniform vec3 starPos;

in vec2 texCoord;
out vec4 fragColor;

vec3 reconstructViewPos(vec2 uv, float depth)
{
    vec4 pos_clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 pos_view = invProjMat * pos_clip;
    return pos_view.xyz / pos_view.w;
}

vec3 computeSmoothNormal(vec2 uv)
{
    float depthC = texture(MainDepthSampler, uv).r;
    vec3 center = reconstructViewPos(uv, depthC);

    vec2 texel = 1.0 / vec2(textureSize(MainDepthSampler, 0));

    // sample in a cross pattern (center + 4 neighbors)
    vec3 positions[5];
    positions[0] = center;

    float threshold = 0.01; // depth difference threshold

    // right
    float depthR = texture(MainDepthSampler, uv + vec2(texel.x, 0.0)).r;
    positions[1] = (abs(depthR - depthC) < threshold) ? reconstructViewPos(uv + vec2(texel.x, 0.0), depthR) : center;

    // left
    float depthL = texture(MainDepthSampler, uv - vec2(texel.x, 0.0)).r;
    positions[2] = (abs(depthL - depthC) < threshold) ? reconstructViewPos(uv - vec2(texel.x, 0.0), depthL) : center;

    // up
    float depthU = texture(MainDepthSampler, uv + vec2(0.0, texel.y)).r;
    positions[3] = (abs(depthU - depthC) < threshold) ? reconstructViewPos(uv + vec2(0.0, texel.y), depthU) : center;

    // down
    float depthD = texture(MainDepthSampler, uv - vec2(0.0, texel.y)).r;
    positions[4] = (abs(depthD - depthC) < threshold) ? reconstructViewPos(uv - vec2(0.0, texel.y), depthD) : center;

    // compute normal using central differences
    vec3 dx = vec3(positions[1] - positions[2]) * 0.5;
    vec3 dy = vec3(positions[3] - positions[4]) * 0.5;

    return normalize(cross(dx, dy));
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float depth = texture(MainDepthSampler, texCoord).r;
    vec4 planetMask = texture(PlanetMaskSampler, texCoord);
    float planetDepth = texture(PlanetDepthSampler, texCoord).r;

    // Skip post-processing for planet pixels that are visible (not occluded)
    // planetMask.r = 1.0 means planet was rendered here
    // planetMask.g = planet's depth (encoded as color by mask shader)
    // Since both use gl_FragCoord.z, they should match exactly for same geometry
    // Use very tight threshold - only skip if depths are essentially equal
    if (planetMask.r > 0.5 && abs(depth - planetMask.g) < 0.000001) {
        fragColor = color;
        return;
    }

    if (depth < 1.0) {
        vec3 P_view3 = reconstructViewPos(texCoord, depth);
        float dist = length(P_view3);

        // Fade out effect: full at <512, fading 512-1024, none at >1024
        float fade = 1.0 - smoothstep(512.0, 1024.0, dist);

        if (fade > 0.0) {
            vec3 normalView = computeSmoothNormal(texCoord);
            vec3 normalWorld = normalize(mat3(invViewMat) * normalView);

            vec3 P_world = (invViewMat * vec4(P_view3, 1.0)).xyz;
            vec3 light_vec = normalize((P_world + cameraPos) - starPos);

            float brightness = clamp(1.0 - (2 * dot(light_vec, normalWorld)), 1, 2);
            // Lerp brightness toward 1.0 (no effect) as fade decreases
            brightness = mix(1.0, brightness, fade);

            fragColor = pow(color, 1.0 / vec4(brightness, brightness, brightness, 1.0));
        } else {
            fragColor = color;
        }
    } else {
        fragColor = color;
    }
}
