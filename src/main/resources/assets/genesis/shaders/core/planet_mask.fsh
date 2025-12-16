#version 150

out vec4 fragColor;

void main() {
    // R = 1.0 indicates planet is present at this pixel
    // G = depth value (gl_FragCoord.z is in [0,1] range after perspective divide)
    fragColor = vec4(1.0, gl_FragCoord.z, 0.0, 1.0);
}
