in vec4 texCoord0;

out vec4 frag_color;

void sun() {
    float dist = max(abs(0.5 - texCoord0.z), abs(0.5 - texCoord0.w));
    frag_color = vec4(1, 1 - (dist * 0.5), 1 - (dist * 1.5), 1);
}

void planet() {
    float dist = max(abs(0.5 - texCoord0.z), abs(0.5 - texCoord0.w));
    frag_color = vec4(0, 0.5 - (dist * 0.2), 0.6 - (dist * 0.3), 1);
}

void main() {
    if (texCoord0.x == 0) {
        sun();
    } else if (texCoord0.x == 1) {
        planet();
    }
}