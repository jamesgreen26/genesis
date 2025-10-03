#version 150

in vec2 texCoord0;
in vec4 vertexColor; // xyz coords in .rgb (0-1)

out vec4 frag_color;

// --- Simple hash noise ---
float hash(vec3 p) {
    // dot with arbitrary prime constants
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

// Smooth noise from hashing
float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    // 8 corners of cube
    float n000 = hash(i + vec3(0.0,0.0,0.0));
    float n100 = hash(i + vec3(1.0,0.0,0.0));
    float n010 = hash(i + vec3(0.0,1.0,0.0));
    float n110 = hash(i + vec3(1.0,1.0,0.0));
    float n001 = hash(i + vec3(0.0,0.0,1.0));
    float n101 = hash(i + vec3(1.0,0.0,1.0));
    float n011 = hash(i + vec3(0.0,1.0,1.0));
    float n111 = hash(i + vec3(1.0,1.0,1.0));

    // Smooth interpolation
    vec3 u = f*f*(3.0 - 2.0*f);

    return mix(
        mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
        mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y),
        u.z
    );
}

void main() {
    float dist = max(abs(0.5 - texCoord0.x), abs(0.5 - texCoord0.y));

    // Use xyz coords from vertexColor as seed for noise
    vec3 pos = vertexColor.rgb * 8.0; // scale up for more detail
    float n = noise(pos * 3.0);

    // Swirly effect: perturb with sine/cos waves
    float swirl = sin(pos.x * 8.0 + n * 6.283) * 0.5 + 0.5;

    // Combine base color with noise
    float c = 1.0 - dist * 0.6;
    vec3 baseColor = vec3(0.3, 0.3, 0.3);

    vec3 finalColor = mix(baseColor, baseColor * (0.5 + 0.5*swirl), 0.6);

    frag_color = vec4(finalColor, 1.0);
}
