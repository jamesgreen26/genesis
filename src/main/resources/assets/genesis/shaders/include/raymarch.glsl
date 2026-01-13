#version 150

in vec3 vWorldPos;
in vec3 vViewPos;
in vec3 Position;
in vec3 vCameraPos;
in vec4 vColor;
in vec3 vNormal;

uniform int vPlanetIndex;
uniform mat4 ModelViewMat;
uniform mat4 InverseViewMatrix;

// uniform mat4 ProjMat;

#moj_import <genesis:planetdata.glsl>


mat3 rotX(float a) {
    float s = sin(a), c = cos(a);
    return mat3(1, 0, 0, 0, c, -s, 0, s, c);
}

mat3 rotY(float a) {
    float s = sin(a), c = cos(a);
    return mat3(c, 0, s, 0, 1, 0, -s, 0, c);
}

mat3 rotZ(float a) {
    float s = sin(a), c = cos(a);
    return mat3(c, -s, 0, s, c, 0, 0, 0, 1);
}

mat3 makeRot(vec3 r) {
    return rotZ(r.z) * rotY(r.y) * rotX(r.x);
}

#define MAX_MARCH_DIST 1000000000000000000000000000000.0
#define MAX_STEPS 1024
#define PI 3.14159265359
#define SOFT_SHADOW_SAMPLES 48

/* ---------- SDFs ---------- */
float box(vec3 p, vec3 b, float r) {
    p = abs(p) - b + r;
    return length(max(p, vec3(0.0))) + min(max(max(p.x, p.y), p.z), 0.0) - r;
}

float sceneSDF(vec3 p, out int hitPlanet) {
    float dMin = 1e20;
    hitPlanet = -1;

    for(int i = 0; i < NUM_PLANETS; i++) {
        float d = box(makeRot(planetRot[i]) * (p - planetPos[i]), planetHalfSize[i], planetRoundedness[i]);

        if(d < dMin) {
            dMin = d;
            hitPlanet = i;
        }
    }
    return dMin;
}

float emissiveSDF(vec3 p, out int hitLight) {
    float dMin = 1e20;
    hitLight = -1;

    for(int i = 0; i < NUM_SUNS; i++) {
        float d = box(makeRot(sunRot[i]) * (p - sunPos[i]), sunHalfSize[i], sunRoundedness[i]);
        if(d < dMin) {
            dMin = d;
            hitLight = i;
        }
    }
    return dMin;
}

float hash12(vec2 p)
{
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float shadowMap(vec3 p, int targetSun) {
    float dMin = 1e20;
    // hitPlanet = -1;

    for(int i = 0; i < NUM_PLANETS; i++) {
        // if its further than the current planet, then it obviously can't shadow it
        if (i == vPlanetIndex)
            continue;
        float d = box(makeRot(planetRot[i]) * (p - planetPos[i]), planetHalfSize[i], planetRoundedness[i]);

        dMin = min(dMin, d);
    }
    return dMin;
}

float softShadow(vec3 ro, vec3 rd, float maxDist, int targetSun)
{
    // Pixel-stable jitter
    vec2 pixel = floor(gl_FragCoord.xy);
    float jitter = hash12(pixel);

    // Initial ray parameter (avoid self-intersection)
    float t = max(0.05, planetHalfSize[vPlanetIndex].x * 1e-5);
    t += jitter * t;

    float res = 1.0;

    for (int i = 0; i < 64; i++)
    {
        if (t >= maxDist)
            break;

        vec3 p = ro + rd * t;

        // Signed distance to nearest blocking planet along ray
        float h = shadowMap(p, targetSun);

        // Hard contact shadow
        if (h < 0.001)
            return 0.0;

        // Penumbra estimate (IQ-style)
        // Clamp avoids explosion near t ≈ 0
        res = min(res, 2.5 * clamp(h / max(t, 0.01), 0.0, 1.0));

        // --- Scale-aware stepping ---
        // Large steps in empty space, conservative near geometry
        float step = min(h, 0.25 * t);

        // Prevent stagnation
        step = max(step, planetHalfSize[vPlanetIndex].x * 1e-5);

        t += step;
    }

    return clamp(res, 0.0, 1.0);
}


vec3 boxNormal(vec3 p, vec3 b) {
    // Compute distances to box faces along each axis
    vec3 d = b - abs(p); // distance from point to each face

    // Find the axis with the smallest distance → closest face
    if(d.x < d.y && d.x < d.z)
        return vec3(sign(p.x), 0.0, 0.0);
    if(d.y < d.z)
        return vec3(0.0, sign(p.y), 0.0);
    return vec3(0.0, 0.0, sign(p.z));
}

vec3 roundedBoxNormal(vec3 p, vec3 b, float r) {
    if(r == 0.)
        return boxNormal(p, b);
    // match SDF coordinates exactly
    vec3 q = abs(p) - b + r;

    // rounded region (edges + corners)
    if(max(q.x, max(q.y, q.z)) > 0.0) {
        return normalize(sign(p) * max(q, 0.0));
    }

    // planar faces
    vec3 a = abs(p);
    if(a.x > a.y && a.x > a.z)
        return vec3(sign(p.x), 0.0, 0.0);
    if(a.y > a.z)
        return vec3(0.0, sign(p.y), 0.0);
    return vec3(0.0, 0.0, sign(p.z));
}

// vec3 calcSdfNormal(vec3 pos)
// {
//     int unused = -1;
//     vec3 n = vec3(0.0);
//     for (int i = 0; i < 4; i++)
//     {
//         vec3 e = 0.5773 * (2.0 * vec3(
//             (((i+3)>>1)&1),
//             ((i>>1)&1),
//             (i&1)
//         ) - 1.0);
//         n += e * sceneSDF(pos + 0.0005 * e, unused);
//     }
//     return normalize(n);
// }

// maybe use https://www.shadertoy.com/view/WlSXRW

/* ---------- Raymarch ---------- */
float rayMarch(vec3 ro, vec3 rd, out vec3 hitPos, out int hitLight, out int hitPlanet) {
    float t = 0.0;
    hitLight = -1;
    hitPlanet = -1;

    for(int i = 0; i < MAX_STEPS; i++) {
        vec3 p = ro + rd * t;
        int tmpPlanet;
        float dScene = sceneSDF(p, tmpPlanet);
        int tmpLight;
        float dEmit = emissiveSDF(p, tmpLight);

        if(dScene < 0.001) {
            hitPlanet = tmpPlanet;
            hitLight = -1;
            hitPos = p;
            return t;
        }
        if(dEmit < 0.001) {
            hitPlanet = -1;
            hitLight = tmpLight;
            hitPos = p;
            return t;
        }

        t += min(dScene, dEmit);
        if(t > MAX_MARCH_DIST)
            break;
    }

    hitPos = ro + rd * t;
    return t;
}

vec2 boxUV(vec3 p, vec3 n, vec3 halfSize) {
    vec3 an = abs(n); // dominant axis detection

    // Planar faces
    if(an.x > an.y && an.x > an.z)        // ±X face
        return (p.yz + halfSize.yz) / (2.0 * halfSize.yz);
    else if(an.y > an.x && an.y > an.z)   // ±Y face
        return (p.xz + halfSize.xz) / (2.0 * halfSize.xz);
    else                                   // ±Z face
        return (p.xy + halfSize.xy) / (2.0 * halfSize.xy);
}

vec2 roundedBoxUV(vec3 p, vec3 n, vec3 halfSize, float r) {
    if(r == 0.)
        return boxUV(p, n, halfSize);
    // Identify dominant normal axis
    vec3 an = abs(n);

    // ---------- PLANAR FACES ----------
    if(an.x > an.y && an.x > an.z) {
        // ±X face
        return (p.yz + halfSize.yz) / (2.0 * halfSize.yz);
    } else if(an.y > an.x && an.y > an.z) {
        // ±Y face
        return (p.xz + halfSize.xz) / (2.0 * halfSize.xz);
    } else if(an.z > an.x && an.z > an.y) {
        // ±Z face
        return (p.xy + halfSize.xy) / (2.0 * halfSize.xy);
    }

    // ---------- EDGE PATCHES ----------
    // Two normals dominant → cylindrical blend
    if(an.x > 0.0 && an.y > 0.0 && an.z < max(an.x, an.y)) {
        float theta = atan(n.y, n.x);
        return vec2((p.z + halfSize.z) / (2.0 * halfSize.z), theta / (0.5 * PI));
    }

    if(an.x > 0.0 && an.z > 0.0 && an.y < max(an.x, an.z)) {
        float theta = atan(n.z, n.x);
        return vec2((p.y + halfSize.y) / (2.0 * halfSize.y), theta / (0.5 * PI));
    }

    if(an.y > 0.0 && an.z > 0.0 && an.x < max(an.y, an.z)) {
        float theta = atan(n.z, n.y);
        return vec2((p.x + halfSize.x) / (2.0 * halfSize.x), theta / (0.5 * PI));
    }

    // ---------- CORNER PATCHES ----------
    // All three components significant → spherical corner
    vec3 c = sign(p) * (halfSize - r);
    vec3 v = normalize(p - c);

    float phi = acos(clamp(v.z, -1.0, 1.0));   // polar
    float theta = atan(v.y, v.x);                 // azimuth

    return vec2(theta / (0.5 * PI), phi / (0.5 * PI));
}

int boxFace(vec3 n) {
    vec3 a = abs(n);

    if(a.x > a.y && a.x > a.z)
        return n.x < 0.0 ? 1 : 3; // east / west
    if(a.y > a.z)
        return n.y > 0.0 ? 5 : 4; // up / down
    return n.z > 0.0 ? 0 : 2;     // north / south
}

ivec2 faceTile(int face) {
    if(face == 0)
        return ivec2(0, 1); // north
    if(face == 1)
        return ivec2(0, 0); // east
    if(face == 2)
        return ivec2(2, 1); // south
    if(face == 3)
        return ivec2(1, 1); // west
    if(face == 4)
        return ivec2(1, 0); // down
    return ivec2(2, 0);                // up
}

vec2 atlasUV(vec2 faceUV, ivec2 tile) {
    vec2 tileSize = vec2(1.0 / 3.0, 1.0 / 2.0);
    return tileSize * (vec2(tile) + faceUV);
}

// vec4 getTex(vec3 p, int i) {
//     mat3 Rp = makeRot(planetRot[i]);
//     vec3 lp = Rp * (p - planetPos[i]);

//     vec3 n = roundedBoxNormal(lp, planetHalfSize[i], planetRoundedness[i]);

//     vec2 faceUV = roundedBoxUV(lp, n, planetHalfSize[i], planetRoundedness[i]);

//     // this is ideally not needed, the texture mapping should probably be fixed
//     int face = boxFace(n);
//     if(face != 0 && face != 3) {
//         faceUV.x = 1.0 - faceUV.x;
//     }

//     if(face == 1) {
//         faceUV = vec2(faceUV.y, 1.0 - faceUV.x);
//     }
//     if(face == 3) {
//         faceUV = vec2(1.0 - faceUV.y, faceUV.x);
//     }

//     ivec2 tile = faceTile(face);

//     vec2 uv = atlasUV(faceUV, tile);

//     if(i == 0)
//         return texture(Sampler0, uv);
//     if(i == 1)
//         return texture(Sampler0, uv);
//     return vec4(1, 0, 1, 1);
// }

vec2 boxIntersection(in vec3 ro, in vec3 rd, vec3 boxSize, out vec3 outNormal) {
    vec3 m = 1.0 / rd; // can precompute if traversing a set of aligned boxes
    vec3 n = m * ro;   // can precompute if traversing a set of aligned boxes
    vec3 k = abs(m) * boxSize;
    vec3 t1 = -n - k;
    vec3 t2 = -n + k;
    float tN = max(max(t1.x, t1.y), t1.z);
    float tF = min(min(t2.x, t2.y), t2.z);
    if(tN > tF || tF < 0.0)
        return vec2(-1.0); // no intersection

    outNormal = (tN > 0.0) ? step(vec3(tN), t1) : // ro ouside the box
    step(t2, vec3(tF));  // ro inside the box
    outNormal *= -sign(rd);
    return vec2(tN, tF);
}

// IQ from shadertoy
vec2 roundedboxIntersect(
    in vec3 ro,
    in vec3 rd,
    in vec3 size,
    in float rad,
    out vec3 outNormal
) {
    outNormal = vec3(0.0);

    if(rad == 0.0) {
        return boxIntersection(ro, rd, size, outNormal);
    }

    // ---------- Bounding box intersection ----------
    vec3 m = 1.0 / rd;
    vec3 n = m * ro;
    vec3 k = abs(m) * (size + rad);
    vec3 t1 = -n - k;
    vec3 t2 = -n + k;
    float tN = max(max(t1.x, t1.y), t1.z);
    float tF = min(min(t2.x, t2.y), t2.z);
    if(tN > tF || tF < 0.0)
        return vec2(-1.0); // no intersection

    float t = tN;

    // ---------- Convert to first octant ----------
    vec3 pos = ro + t * rd;
    vec3 s = sign(pos);
    ro *= s;
    rd *= s;
    pos *= s;

    // ---------- Check planar faces ----------
    vec3 local = pos - size;
    local = max(local.xyz, local.yzx);
    if(min(min(local.x, local.y), local.z) < 0.0) {
        if(local.x > local.y && local.x > local.z)
            outNormal = vec3(1.0, 0.0, 0.0);
        else if(local.y > local.z)
            outNormal = vec3(0.0, 1.0, 0.0);
        else
            outNormal = vec3(0.0, 0.0, 1.0);
        outNormal *= s; // transform back to world space
        return vec2(tN, tF);
    }

    // ---------- Precompute for edges/corners ----------
    vec3 oc = ro - size;
    vec3 dd = rd * rd;
    vec3 oo = oc * oc;
    vec3 od = oc * rd;
    float ra2 = rad * rad;

    t = 1e30;

    // ---------- Corner ----------
    {
        float b = od.x + od.y + od.z;
        float c = oo.x + oo.y + oo.z - ra2;
        float h = b * b - c;
        if(h > 0.0) {
            float tc = -b - sqrt(h);
            if(tc > 0.0 && tc < t) {
                t = tc;
                vec3 pCorner = ro + rd * t - size;
                outNormal = normalize(pCorner);
                outNormal *= s;
            }
        }
    }

    // ---------- Edge X ----------
    {
        float a = dd.y + dd.z;
        float b = od.y + od.z;
        float c = oo.y + oo.z - ra2;
        float h = b * b - a * c;
        if(h > 0.0) {
            float te = (-b - sqrt(h)) / a;
            if(te > 0.0 && te < t && abs(ro.x + rd.x * te) < size.x) {
                t = te;
                vec3 pEdge = ro + rd * t - vec3(size.x, 0.0, 0.0);
                pEdge.yz = normalize(pEdge.yz) * rad;
                outNormal = normalize(vec3(0.0, pEdge.y, pEdge.z));
                outNormal *= s;
            }
        }
    }

    // ---------- Edge Y ----------
    {
        float a = dd.z + dd.x;
        float b = od.z + od.x;
        float c = oo.z + oo.x - ra2;
        float h = b * b - a * c;
        if(h > 0.0) {
            float te = (-b - sqrt(h)) / a;
            if(te > 0.0 && te < t && abs(ro.y + rd.y * te) < size.y) {
                t = te;
                vec3 pEdge = ro + rd * t - vec3(0.0, size.y, 0.0);
                pEdge.xz = normalize(pEdge.xz) * rad;
                outNormal = normalize(vec3(pEdge.x, 0.0, pEdge.z));
                outNormal *= s;
            }
        }
    }

    // ---------- Edge Z ----------
    {
        float a = dd.x + dd.y;
        float b = od.x + od.y;
        float c = oo.x + oo.y - ra2;
        float h = b * b - a * c;
        if(h > 0.0) {
            float te = (-b - sqrt(h)) / a;
            if(te > 0.0 && te < t && abs(ro.z + rd.z * te) < size.z) {
                t = te;
                vec3 pEdge = ro + rd * t - vec3(0.0, 0.0, size.z);
                pEdge.xy = normalize(pEdge.xy) * rad;
                outNormal = normalize(vec3(pEdge.x, pEdge.y, 0.0));
                outNormal *= s;
            }
        }
    }

    if(t > 1e29)
        return vec2(-1.0);
    return vec2(tN, tF);
}

bool shadowRayAnalytic(
    vec3 ro,
    vec3 rd,
    float maxDist,
    // int targetSun,
    out vec3 normal
) {
    // Scale-aware bias to reduce self-shadowing
    ro += rd * max(0.001, planetHalfSize[vPlanetIndex].x * 1e-5);

    for (int j = 0; j < NUM_PLANETS; j++) {
        // Skip self
        if (j == vPlanetIndex)
            continue;

        // --- Cheap ray-space culling ---
        // Project planet center onto shadow ray
        vec3 toPlanet = planetPos[j] - ro;
        float proj = dot(toPlanet, rd);

        // Planet is behind the ray origin or beyond the light
        if (proj <= 0.0 || proj >= maxDist)
            continue;

        // --- Transform ray into planet local space ---
        mat3 Rp = makeRot(planetRot[j]);
        vec3 localRo = Rp * (ro - planetPos[j]);
        vec3 localRd = Rp * rd;

        // --- Local-space intersection ---
        vec2 tHit = roundedboxIntersect(
            localRo,
            localRd,
            planetHalfSize[j],
            planetRoundedness[j],
            normal
        );

        float tNear = tHit.x;

        // --- Valid blocker ---
        if (tNear > 0.0 && tNear < maxDist) {
            // Transform normal back to world space
            normal = normalize(transpose(Rp) * normal);
            return false; // BLOCKED
        }
    }

    return true; // CLEAR
}


// if the occluder is too close to whats getting occluded, it skips it entirely

float softBoxShadow(vec3 p, int targetSun) {
    float sum = 0.0;

    // Simple stratified sampling on box surface
    for(int i = 0; i < SOFT_SHADOW_SAMPLES; i++) {
        // generate a random point on the box surface
        vec3 offset = vec3((fract(sin(float(i) * 12.9898) * 43758.5453) - 0.5) * 2.0 * sunHalfSize[targetSun].x, (fract(sin(float(i) * 78.233) * 43758.5453) - 0.5) * 2.0 * sunHalfSize[targetSun].y, (fract(sin(float(i) * 39.425) * 43758.5453) - 0.5) * 2.0 * sunHalfSize[targetSun].z);

        vec3 lightPoint = sunPos[targetSun] + offset;

        // shadow ray
        vec3 dir = normalize(lightPoint - p);
        float dist = length(lightPoint - p);

        int tmp;
        vec3 n;
        bool blocked = !shadowRayAnalytic(p, dir, dist, n);

        sum += blocked ? 0.0 : 1.0;
    }

    return sum / float(SOFT_SHADOW_SAMPLES);
}

vec3 projectPointToBox(vec3 p, vec3 boxCenter, vec3 halfSize) {
    // Move point into box local space
    vec3 local = p - boxCenter;

    // Clamp to box extents
    vec3 clamped = clamp(local, -halfSize, halfSize);

    // Transform back to world space
    return clamped + boxCenter;
}

vec3 projectPointToRoundedBox(vec3 p, vec3 boxCenter, vec3 halfSize, float r) {
    if(r == 0.)
        return projectPointToBox(p, boxCenter, halfSize);
    // Move point into box local space
    vec3 local = p - boxCenter;

    // Inner box: halfSize reduced by rounding radius
    vec3 innerBox = halfSize - vec3(r);

    // Clamp to inner box
    vec3 clamped = clamp(local, -innerBox, innerBox);

    // Offset from clamped point
    vec3 offset = local - clamped;

    // If outside inner box, project onto rounded region
    float len = length(offset);
    if(len > r)
        offset = (offset / len) * r;

    // Transform back to world space
    return clamped + offset + boxCenter;
}

vec3 calcLighting(vec3 base) {
    vec3 p = vViewPos;
    

    // mat3 Rp = makeRot(planetRot[vPlanetIndex]);
    // vec3 lp = Rp * (p - planetPos[vPlanetIndex]);

    // vec3 nLocal = roundedBoxNormal(lp, planetHalfSize[vPlanetIndex], planetRoundedness[vPlanetIndex]);
    // vec3 n = normalize(transpose(Rp) * nLocal);
    // base += n;

    // ambient in the original so i'll do it here too
    vec3 lighting = vec3(0.05);

    for(int i = 0; i < NUM_SUNS; i++) {
        mat3 Rs = makeRot(sunRot[i]);
        vec3 lpL = Rs * (p - sunPos[i]);
        vec3 q = projectPointToRoundedBox(lpL, vec3(0.0), sunHalfSize[i], sunRoundedness[i]);
        vec3 closestPoint = transpose(Rs) * q + sunPos[i];

        vec3 Lvec = closestPoint - p;
        vec3 L = normalize(Lvec);
        float dist = length(Lvec);

        float ndotl = dot(vNormal, L);
        if(ndotl <= 0.0)
            continue;

        float sh = softBoxShadow(p, i);
        // float sh = softShadow(p, L, dist, i);
        lighting += sunCol[i] * sh * ndotl;
    }

    return lighting;
}