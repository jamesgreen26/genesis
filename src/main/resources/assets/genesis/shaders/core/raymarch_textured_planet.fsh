#version 150
#moj_import <genesis:raymarch.glsl>
// above should be copied from raymarch.glsl
uniform sampler2D Sampler0;
in vec2 texCoord;

out vec4 frag_color;

void main() {
    vec3 base = texture(Sampler0, texCoord).xyz;
    // frag_color = vec4(fract(vWorldPos*0.001), 1.0);
    // return;
    
    frag_color = vec4(base * calcLighting(base), 1.0);
}