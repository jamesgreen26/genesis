#version 150

#moj_import <genesis:dither.glsl>
#moj_import <genesis:shadow_volumes.glsl>

// Light that reaches the whole surface, whether or not it faces the star
const float AMBIENT_LIGHT = 0.1;
// How much of the star's light a shadow blocks
const float SHADOW_STRENGTH = 0.77;

in vec2 texCoord;
in vec3 lightDir;

in vec3 v_entry_position;
in vec3 surfacePos;

uniform sampler2D Sampler0;
// Emissive texture (1x1 transparent black if the planet has none)
uniform sampler2D Sampler1;

// The centre of the cube, in the same space as surfacePos and the shadow planes
uniform vec3 ShadowOrigin;

out vec4 frag_color;

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Simple directional lighting from the passed-in normal
    vec3 n = normalize(v_entry_position);
    float ndotl = max(dot(n, -normalize(lightDir)), 0.0);

    ndotl = 1. - 1. / (ndotl * ndotl * 5. + 1.);

    // A shadow takes away the star's light and leaves the rest
    ndotl *= 1.0 - SHADOW_STRENGTH * shadowCoverage(surfacePos - ShadowOrigin);

    float lighting = clamp(AMBIENT_LIGHT + ndotl, 0.0, 1.0);

    vec3 litTexColor = texColor.rgb * lighting;

    // Emissive light is unaffected by lighting and shadows; alpha controls its strength
    vec4 emissive = texture(Sampler1, texCoord);
    litTexColor += emissive.rgb * emissive.a;

    // Planets are always fully opaque
    frag_color = vec4(dither(litTexColor), 1.0);
    // Depth is set by the depth range the celestial is drawn with
}
