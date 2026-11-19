#version 150

#moj_import <genesis:dither.glsl>
#moj_import <genesis:shadow_volumes.glsl>

// Light that reaches the whole ring, whether or not it's in shadow. The same as on the cube the ring is around.
const float AMBIENT_LIGHT = 0.1;

// x: how far along its side of the ring this is from the middle of the side, in units of the ring's width
// y: from 0 at the ring's outer edge to 1 at its inner edge
in vec2 texCoord;
// In the celestial's local space, as are the camera and the shadow planes
in vec3 localPos;

uniform sampler2D Sampler0;
uniform vec3 CameraPosition;
// Half the size of the cube the ring is around
uniform float HalfSize;

out vec4 fragColor;

// Whether the cube is between the camera and this part of the ring. A celestial is drawn at a single depth, so the
// depth test can't tell.
bool behindCube() {
    vec3 ray = localPos - CameraPosition;
    vec3 t1 = (-HalfSize - CameraPosition) / ray;
    vec3 t2 = (HalfSize - CameraPosition) / ray;
    vec3 tMin = min(t1, t2);
    vec3 tMax = max(t1, t2);
    float tNear = max(max(tMin.x, tMin.y), tMin.z);
    float tFar = min(min(tMax.x, tMax.y), tMax.z);
    // The ray reaches this part of the ring at 1
    return tNear < tFar && tFar > 0.0 && tNear < 1.0;
}

void main() {
    if (behindCube()) discard;

    // The texture is a slice of the ring, repeated along it with its pixels kept square, and with the middle of a
    // slice in the middle of each side
    ivec2 size = textureSize(Sampler0, 0);
    vec4 color = texture(Sampler0, vec2(0.5 + texCoord.x * float(size.y) / float(size.x), texCoord.y));
    if (color.a <= 0.0) discard;

    // A shadow takes away all of the star's light and leaves the rest, making the ring as dark in shadow as the night
    // side of the cube it's around
    float lighting = AMBIENT_LIGHT + (1.0 - AMBIENT_LIGHT) * (1.0 - shadowCoverage(localPos));

    // Premultiplied, so the dithering isn't faded out along with the ring
    fragColor = vec4(dither(color.rgb * lighting * color.a), color.a);
}
