#version 150

in vec3 v_camera_pos;
in vec3 v_entry_position;

out vec4 frag_color;

const vec3 v_cube_center = vec3(0);

const float cube_half_size = 720.0;
const vec3 cube_rotationXYZ = vec3(0, 0, 0);
const float euler = 2.718281828459;

float smoothNormalize(float it) {
    return -log(1/euler + pow(euler, -4 * (it + 0.11467)));
}

vec3 temperatureToColor(float t) {
    t = clamp(t, 0.0, 1.0);

    const vec3 red    = vec3(1.0, 0.0, 0.0);
    const vec3 orange = vec3(1.0, 0.5, 0.0);
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

float rayBoxIntersection(vec3 rayStart, vec3 rayDir, vec3 boxCenter, float boxHalfSize, mat3 rotation) {
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
        return -1.0;
    }

    return tExit;
}

void main() {
    vec3 ray_direction = normalize(v_entry_position - v_camera_pos);

    mat3 rot = rotationMatrix(cube_rotationXYZ);

    float exit_distance = rayBoxIntersection(v_entry_position, ray_direction, v_cube_center, cube_half_size, rot);

    float thickness = exit_distance;

    if (thickness < 0.0) {
        thickness = 0.0;
    }

    vec3 ray_midpoint = v_entry_position + ray_direction * (thickness * 0.5);

    vec3 diff = ray_midpoint - v_cube_center;

    float distanceFromCenter = max(max(abs(diff.x), abs(diff.y)), abs(diff.z)) * 2.5;

    float maxDistance = cube_half_size * 2 * 1.732;
    float normalizedDistance = clamp(distanceFromCenter / maxDistance, 0.0, 1.0);

    float brightness = min(1, 1 - normalizedDistance);

    brightness = brightness * brightness;

    float maxThickness = cube_half_size * 1.732;
    float normalizedThickness = thickness / maxThickness;

    float temp = mix(smoothNormalize(normalizedThickness), 1.5, smoothNormalize(brightness));

    vec3 color = temperatureToColor(temp / 1.5);

    frag_color = vec4(color, clamp(normalizedThickness * normalizedThickness * 4, 0, 1));
}