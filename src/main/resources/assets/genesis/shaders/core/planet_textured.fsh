#version 150

in vec2 texCoord;
in vec3 vNormal;
in vec3 lightDir;

in vec3 v_entry_position;

uniform sampler2D Sampler0;
// Emissive texture (1x1 transparent black if the planet has none)
uniform sampler2D Sampler1;

out vec4 frag_color;

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Simple directional lighting from the passed-in normal
    vec3 n = normalize(v_entry_position);
    float ndotl = max(dot(n, -normalize(lightDir)), 0.0);

    ndotl = 1. - 1. / (ndotl * ndotl * 5. + 1.);

    float ambient = 0.1;
    float lighting = clamp(ambient + ndotl, 0.0, 1.0);

    vec3 litTexColor = texColor.rgb * lighting;

    // Emissive light is unaffected by lighting; alpha controls its strength
    vec4 emissive = texture(Sampler1, texCoord);
    litTexColor += emissive.rgb * emissive.a;

    // Planets are always fully opaque
    frag_color = vec4(litTexColor, 1.0);
    // Depth is set by the depth range the celestial is drawn with
}
