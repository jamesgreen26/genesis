#version 150

in vec2 texCoord;
in vec4 vertexColor;
in vec3 vNormal;
in vec3 lightDir;

uniform sampler2D Sampler0;

out vec4 frag_color;

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Interpolate between fog color and texture color based on alpha
    vec3 baseColor = mix(vertexColor.rgb, texColor.rgb, vertexColor.a);

    // Simple directional lighting from the passed-in normal
    vec3 n = normalize(vNormal);
    float ndotl = max(dot(n, -normalize(lightDir)), 0.0);

    float ambient = 0.15;
    float lighting = clamp(ambient + ndotl, 0.0, 1.0);

    vec3 finalColor = baseColor * lighting;

    // Planets are always fully opaque
    frag_color = vec4(finalColor, 1.0);
}
