#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainDepthSampler;

uniform mat4 invProjMat;
uniform mat4 invViewMat;
uniform vec3 cameraPos;

const vec3 LightPos = vec3(0.0);

in vec2 texCoord;
out vec4 fragColor;

vec3 ReconstructWorldPos(float depth, vec2 uv)
{
    vec4 ndc = vec4(
    uv * 2.0 - 1.0,
    depth * 2.0 - 1.0,
    1.0
    );

    vec4 viewPos = invProjMat * ndc;
    viewPos /= viewPos.w;

    vec4 worldPos = invViewMat * viewPos;
    return worldPos.xyz;
}

vec3 ComputeNormal(vec2 uv)
{
    float depth = texture(MainDepthSampler, uv).r;
    vec3 p  = ReconstructWorldPos(depth, uv);  // world space

    float dx = 1.0 / 1920.0;
    float dy = 1.0 / 1080.0;

    float depthX = texture(MainDepthSampler, uv + vec2(dx,0)).r;
    float depthY = texture(MainDepthSampler, uv + vec2(0,dy)).r;

    vec3 px = ReconstructWorldPos(depthX, uv + vec2(dx,0));
    vec3 py = ReconstructWorldPos(depthY, uv + vec2(0,dy));

    // Raw world-space derivatives
    vec3 dxv = px - p;
    vec3 dyv = py - p;

    // Now cross product *is actually world-space*
    return normalize(cross(dxv, dyv));
}


void main()
{
    vec3 baseColor = texture(DiffuseSampler, texCoord).rgb;

    float depth = texture(MainDepthSampler, texCoord).r;
    if (depth >= 1.0) {
        fragColor = vec4(baseColor, 1.0);
        return;
    }

    vec3 worldPos = ReconstructWorldPos(depth, texCoord);
    vec3 normal   = ComputeNormal(texCoord);
    vec3 L        = normalize(LightPos - worldPos);

    float NdotL = dot(normal, L);

    // Clamp for safety
    float lit  = max(NdotL, 0.0);      // light-facing
    float dark = max(-NdotL, 0.0);     // backside

    //---------------------------------------------------------
    // NEW: Balanced harsh-lighting model (no blowout)
    //---------------------------------------------------------

    // Strong but controlled direct light
    float directLight = lit * 1.2;      // boosted but not insane

    // Backside darkening for contrast
    float shadow = 1.0 - dark * 0.5;    // mild darkening

    // Final shaded color
    vec3 finalColor = baseColor * (0.2 + directLight) * shadow;

    // Always clamp for safety
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, 1.0);
}
