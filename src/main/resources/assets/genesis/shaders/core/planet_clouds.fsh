#version 150

in vec3 v_camera_pos;
in vec3 v_entry_position;
in float v_half_size;
in vec3 lightDir;

uniform float CloudThickness;
uniform float Density;
uniform float DensityFade;
uniform float GameTime;

uniform vec3 LightDirection;

const vec4 dayCol = vec4(0.3,0.7,0.9,0.7);
const vec4 sunsetCol = vec4(0.7,0.3,0.1,0.5);
const vec4 nightCol = vec4(0.2,0.1,0.4,0.1);

const float PI = 3.14159265358979;

out vec4 frag_color;

const vec3 v_cube_center = vec3(0);

const vec3 cube_rotationXYZ = vec3(0, 0, 0);
const float euler = 2.718281828459;

float smoothNormalize(float it) {
    return -log(1/euler + pow(euler, -4 * (it + 0.11467)));
}

float smoothMax3(vec3 p, float roundness) {
    vec3 q = abs(p);
    float m = max(max(q.x, q.y), q.z);

    return m;
}


vec3 temperatureToColor(float t) {
    t = clamp(t, 0.0, 1.0);

    const vec3 red    = vec3(1.0, 0.0, 0.0);
    const vec3 orange = vec3(1.0, 0.0, 0.0);
    const vec3 yellow = vec3(1.0, 0.8, 0.15);
    const vec3 white  = vec3(1.0, 1.0, 1.0);

    if (t < 0.33) {
        float k = smoothstep(0.0, 0.33, t);
        return mix(red, orange, k);
    } else if (t < 0.66) {
        float k = smoothstep(0.33, 0.66, t);
        return mix(orange, yellow, k);
    } else {
        float k = smoothstep(0.66, 1.0, t);
        return mix(yellow, white, k);
    }
}

mat3 rotationMatrix(vec3 angles) {
    vec3 rad = radians(angles);
    float cx = cos(rad.x);
    float sx = sin(rad.x);
    float cy = cos(rad.y);
    float sy = sin(rad.y);
    float cz = cos(rad.z);
    float sz = sin(rad.z);

    // Rotation order: X * Y * Z
    mat3 rotX = mat3(
    1.0, 0.0, 0.0,
    0.0, cx, -sx,
    0.0, sx, cx
    );

    mat3 rotY = mat3(
    cy, 0.0, sy,
    0.0, 1.0, 0.0,
    -sy, 0.0, cy
    );

    mat3 rotZ = mat3(
    cz, -sz, 0.0,
    sz, cz, 0.0,
    0.0, 0.0, 1.0
    );

    return rotX * rotY * rotZ;
}

vec2 rayBoxIntersection(vec3 rayStart, vec3 rayDir, vec3 boxCenter, float boxHalfSize, mat3 rotation) {
    mat3 invRotation = transpose(rotation);
    vec3 localRayStart = invRotation * (rayStart - boxCenter);
    vec3 localRayDir = invRotation * rayDir;

    vec3 invDir = 1.0 / localRayDir;
    vec3 t1 = (-boxHalfSize - localRayStart) * invDir;
    vec3 t2 = (boxHalfSize - localRayStart) * invDir;

    vec3 tMin = min(t1, t2);
    vec3 tMax = max(t1, t2);

    float tEntry = max(max(tMin.x, tMin.y), tMin.z);
    float tExit = min(min(tMax.x, tMax.y), tMax.z);

    if (tExit < tEntry || tExit < 0.0) {
        return vec2(-1.0);
    }

    return vec2(tEntry,tExit);
}

float random(float x) {return fract(x * (fract(x * 0.31) + 1.23) * 0.726);}

float random(vec2 x) {return random(x.x + random(x.y) * 73.4);}
float random(vec3 x) {return random(x.x + random(x.yz) * 73.4);}
float random(vec4 x) {return random(x.x + random(x.yzw) * 73.4);}

float cloud(vec3 p) {
    p += floor(vec3(random(floor(p * 64.0) + 0.2),random(floor(p * 64.0) + 0.4),random(floor(p * 64.0) + 0.7)) * 3.0 - 1.0) / 128.0;
    p += floor(vec3(random(floor(p * 64.0) + 0.3),random(floor(p * 64.0) + 0.1),random(floor(p * 64.0) + 0.8)) * 3.0 - 1.0) / 128.0;
    float t = 1.3 * Density - 1.2;
    float q = 2.0;
    vec3 h;
    for(int n = 0; n < 5; n++) {
        h = floor(p * q);
        if(t > 0.0) {
            t -= random(h + 0.3) * 0.85 / q;
        } else {
            t += random(h + 0.6) * 0.4 / q;
        }
        q *= 2.0;
    }
    return t;
}

void main() {
    vec3 p = v_entry_position / CloudThickness / v_half_size * 1.00001;
    p = (floor(p * 128.0) + 0.5) / 128.0;
    p = normalize(p);
    vec3 q = p;
    float g = GameTime * PI * 10.0;
    float cosg = cos(g);
    float sing = sin(g);
    p.xz *= mat2(cosg,sing,-sing,cosg);

    if(cloud(p) > 0.0) {
        frag_color = vec4(1,1,1,1);
        frag_color.a *= clamp(dot(-normalize(LightDirection),q) * 2.3 + 0.4,0.1,0.6);
        frag_color.xyz *= clamp(dot(-normalize(LightDirection),q) * 2.8 + 0.7,0.5,0.9);
    } else {
        frag_color = vec4(0,0,0,0);
    }

    frag_color.a *= DensityFade;
}


