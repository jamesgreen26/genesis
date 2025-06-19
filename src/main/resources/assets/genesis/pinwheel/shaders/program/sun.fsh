in vec2 texCoord0;

out vec4 frag_color;

void main() {
    float dist = max(abs(0.5 - texCoord0.x), abs(0.5 - texCoord0.y));
    frag_color = vec4(1, 1 - (dist * 0.5), 1 - (dist * 1.5), 1);
}