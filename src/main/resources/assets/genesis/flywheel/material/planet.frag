float getHalfSize() {
    return flw_vertexColor.w;
}

vec3 getLocalPos() {
    return flw_vertexNormal.xyz * flw_vertexColor.w;
}

void flw_materialFragment() {
    vec3 localPos = getLocalPos();
    vec3 n = normalize(flw_vertexNormal);
    float ax = abs(n.x);
    float ay = abs(n.y);
    float az = abs(n.z);

    float halfSize = getHalfSize();
    float u_local = 0.5;
    float v_local = 0.5;
    vec2 uv = vec2(0.5, 0.5);

    // Map per-face UVs into the 3x2 atlas used by the CPU renderer
    if (az >= ax && az >= ay) {
        // Z faces
        u_local = (localPos.x / halfSize + 1.0) * 0.5;
        v_local = (localPos.y / halfSize + 1.0) * 0.5;
        if (n.z > 0.0) {
            // +Z -> u:[2/3,1], v:[0,0.5]
            uv.x = mix(2.0/3.0, 1.0, u_local);
            uv.y = mix(0.0, 0.5, v_local);
        } else {
            // -Z -> u:[0,1/3], v:[0,0.5]
            uv.x = mix(0.0, 1.0/3.0, u_local);
            uv.y = mix(0.0, 0.5, v_local);
        }
    } else if (ax >= ay && ax >= az) {
        // X faces
        u_local = (localPos.z / halfSize + 1.0) * 0.5;
        v_local = (localPos.y / halfSize + 1.0) * 0.5;
        if (n.x > 0.0) {
            // +X -> u:[0,1/3], v:[0.5,1]
            uv.x = mix(0.0, 1.0/3.0, u_local);
            uv.y = mix(0.5, 1.0, v_local);
        } else {
            // -X -> u:[1/3,2/3], v:[0,0.5]
            uv.x = mix(1.0/3.0, 2.0/3.0, u_local);
            uv.y = mix(0.0, 0.5, v_local);
        }
    } else {
        // Y faces
        u_local = (localPos.x / halfSize + 1.0) * 0.5;
        v_local = (localPos.z / halfSize + 1.0) * 0.5;
        if (n.y > 0.0) {
            // +Y -> u:[2/3,1], v:[0.5,1]
            uv.x = mix(2.0/3.0, 1.0, u_local);
            uv.y = mix(0.5, 1.0, v_local);
        } else {
            // -Y -> u:[1/3,2/3], v:[0.5,1]
            uv.x = mix(1.0/3.0, 2.0/3.0, u_local);
            uv.y = mix(0.5, 1.0, v_local);
        }
    }

    // Sample the bound texture (Sampler0)
    vec4 color = texture(flw_diffuseTex, uv);
    // vec4 color = vec4(uv, 0.0, 1.0); // Debug: visualize UV mapping

    // flw_fragLight = vec2(1.0);
    vec2 embeddedLight;
    if (flw_lightFetch(ivec3(floor(flw_vertexPos.xyz)) + flw_renderOrigin, embeddedLight)) {
        flw_fragLight = max(flw_fragLight, embeddedLight);
    }
    flw_fragColor = vec4(color.rgb, 1.0);
}
