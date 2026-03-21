#version 150

in vec4 vertexColor;
in vec3 localPos;

uniform float ShadowVertexCount;
uniform float ShadowEdgeMask;
uniform float ShadowEdgeWidth;
uniform vec3 ShadowVertex[8];

out vec4 fragColor;

void main() {
    int count = int(ShadowVertexCount + 0.5);

    if (count <= 0) {
        // No vertices uploaded: show bright magenta as a debug color
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }

    // Degenerate case: a single point, just mark in red for now
    if (count < 2) {
        fragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
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
    //  - After ShadowEdgeWidth into the interior alpha reaches 1.
    // ShadowEdgeWidth is computed CPU-side from the full polygon so all
    // faces sharing a shadow boundary use the same scale.
    float edgeT = clamp(minEdgeDist / ShadowEdgeWidth, 0.0, 1.0);
    float falloff = smoothstep(0.0, 1.0, edgeT);

    fragColor = vec4(vertexColor.rgb, vertexColor.a * falloff);
}
