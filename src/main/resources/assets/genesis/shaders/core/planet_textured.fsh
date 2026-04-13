#version 150

in vec2 texCoord;
in vec4 vertexColor;
in vec3 vNormal;
in vec3 lightDir;

in vec3 v_entry_position;

uniform sampler2D Sampler0;
uniform vec3 SkyColor;

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

    litTexColor += SkyColor * clamp(1 - lighting, 0, 1);

    // Interpolate between fog color and texture color based on alpha
    vec3 finalColor = mix(vertexColor.rgb, litTexColor, vertexColor.a);

    // Planets are always fully opaque
    frag_color = vec4(finalColor, 1.0);
    gl_FragDepth = 1.0;
}
