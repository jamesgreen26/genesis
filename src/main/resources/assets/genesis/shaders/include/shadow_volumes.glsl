// Shadows cast onto a celestial by the others, shared by everything they're drawn on.

// Must match CelestialShadows and ShadowVolume
#define MAX_SHADOW_VOLUMES 4
#define PLANES_PER_SHADOW_VOLUME 7

uniform float ShadowVolumeCount;
// The planes bounding each occluder's shadow volume: dot(xyz, p) + w is how far inside the plane p is, as a fraction of
// the soft edge's width
uniform vec4 ShadowPlane[MAX_SHADOW_VOLUMES * PLANES_PER_SHADOW_VOLUME];

// How deep in shadow p is, from 0 to 1. It depends only on where p is in the shadow volumes, not on what surface it's
// part of, so a shadow's soft edge lines up from one surface to the next.
//
// p: in the space the planes are in
float shadowCoverage(vec3 p) {
    int count = min(int(ShadowVolumeCount + 0.5), MAX_SHADOW_VOLUMES);

    float coverage = 0.0;
    for (int volume = 0; volume < count; ++volume) {
        float depth = 1.0;
        for (int i = 0; i < PLANES_PER_SHADOW_VOLUME; ++i) {
            vec4 plane = ShadowPlane[volume * PLANES_PER_SHADOW_VOLUME + i];
            depth = min(depth, dot(plane.xyz, p) + plane.w);
        }
        // Overlapping shadows don't get any darker than one
        coverage = max(coverage, smoothstep(0.0, 1.0, depth));
    }
    return coverage;
}
