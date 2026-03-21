#version 150

in vec4 vertexColor;
in vec3 localPos;

uniform float ShadowVertexCount;
uniform float ShadowEdgeMask;
uniform vec3 ShadowVertex[8];

out vec4 fragColor;

void main() {
    int count = int(ShadowVertexCount + 0.5);

    if (count <= 0) {
        // No vertices uploaded: show bright magenta as a debug color
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }

    // Compute center of the projected shadow polygon from its vertices
    vec3 center = vec3(0.0);
    for (int i = 0; i < 8; ++i) {
        if (i >= count) break;
        center += ShadowVertex[i];
    }
    center /= float(count);

    // Degenerate case: a single point, just mark in red for now
    if (count < 2) {
        fragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }

    // Approximate a radius from the furthest vertex (used only for scaling)
    float radius = 0.0;
    for (int i = 0; i < 8; ++i) {
        if (i >= count) break;
        vec3 v = ShadowVertex[i];
        float d = length(v - center);
        radius = max(radius, d);
    }

    // Distance to the NEAREST EDGE of the polygon, skipping edges that lie
    // on cube face clipping boundaries (they are not real shadow silhouette edges).
    int mask = int(ShadowEdgeMask + 0.5);
    float minEdgeDist = 1e9;
    for (int i = 0; i < count; ++i) {
        if ((mask & (1 << i)) != 0) continue;
        int j = (i + 1) % count;
        vec3 a = ShadowVertex[i];
        vec3 b = ShadowVertex[j];
        vec3 ab = b - a;
        float len2 = dot(ab, ab);
        if (len2 > 0.0) {
            float t = clamp(dot(localPos - a, ab) / len2, 0.0, 1.0);
            vec3 closest = a + t * ab;
            float d = length(localPos - closest);
            minEdgeDist = min(minEdgeDist, d);
        }
    }

    // Map distance to nearest edge into a soft falloff.
    //  - At the edge (minEdgeDist = 0) alpha is 0.
    //  - After edgeWidth into the interior alpha reaches 1.
    float edgeWidth = max(radius * 0.25, 0.0001);
    float edgeT = clamp(minEdgeDist / edgeWidth, 0.0, 1.0);
    float falloff = smoothstep(0.0, 1.0, edgeT);

    fragColor = vec4(vertexColor.rgb, vertexColor.a * falloff);
}
