#version 150

in vec3 positionData;
in float textureScale;
in vec3 vertexColor;

out vec4 frag_color;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(1.0, 57.0, 113.0))) * 43758.5453);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    float n000 = hash(i + vec3(0,0,0));
    float n100 = hash(i + vec3(1,0,0));
    float n010 = hash(i + vec3(0,1,0));
    float n110 = hash(i + vec3(1,1,0));
    float n001 = hash(i + vec3(0,0,1));
    float n101 = hash(i + vec3(1,0,1));
    float n011 = hash(i + vec3(0,1,1));
    float n111 = hash(i + vec3(1,1,1));

    vec3 u = f*f*(3.0 - 2.0*f);

    return mix(
        mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
        mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y),
        u.z
    );
}

void main() {
    vec3 pos = positionData * 8.0 * textureScale;

    float n = noise(pos * 3.0);

    float swirl = sin(pos.x * 8.0 + n * 6.283) * 0.5 + 0.5;

    vec3 finalColor = mix(vertexColor, vertexColor * (0.5 + 0.5*swirl), 0.6);

    frag_color = vec4(finalColor, 1.0);
}
