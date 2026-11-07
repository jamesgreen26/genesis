#version 150

in vec3 v_camera_pos;
in vec3 v_entry_position;
in float v_half_size;
in vec3 lightDir;

uniform float AtmosphereThickness;
uniform float Density;

const vec4 dayCol = vec4(0.3,0.7,0.9,0.7);
const vec4 sunsetCol = vec4(0.7,0.3,0.1,0.5);
const vec4 nightCol = vec4(0.2,0.1,0.4,0.1);

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

void main() {
    vec3 ray_direction = normalize(v_entry_position - v_camera_pos);

    mat3 rot = rotationMatrix(cube_rotationXYZ);

    float exit_distance = rayBoxIntersection(v_camera_pos, ray_direction, v_cube_center, v_half_size * AtmosphereThickness, rot).y;
    float entry_distance = rayBoxIntersection(v_camera_pos, ray_direction, v_cube_center, v_half_size, rot).x;

    if(entry_distance < -0.1) {
        entry_distance = rayBoxIntersection(v_camera_pos, ray_direction, v_cube_center, v_half_size * AtmosphereThickness, rot).y;
    }

    vec3 enterPos = v_camera_pos + ray_direction * max(rayBoxIntersection(v_camera_pos, ray_direction, v_cube_center, v_half_size * AtmosphereThickness, rot).x,0.0);

    float thickness = 1000000000000.0;

    if (thickness < 0.0) {
        thickness = 0.0;
    }

    // Transform ray to local cube space
    mat3 invRot = transpose(rot);
    vec3 localRayStart = invRot * (v_camera_pos - v_cube_center);
    vec3 localRayDir = normalize(invRot * ray_direction);
    vec3 localCamPos = invRot * v_camera_pos;
    vec3 localIntPos = invRot * (v_camera_pos + entry_distance * ray_direction);
    vec3 localEnterPos = invRot * enterPos;
    vec3 localLightDir = normalize(invRot * lightDir);

    // Check intersection with each of the 6 diagonal planes
    float roundness_factor = 0.5;

    // Calculate minimum of distance components at intersections with 6 diagonal planes
    float minDist = smoothMax3(vec3(abs(localCamPos.x),abs(localCamPos.y),abs(localCamPos.z)),roundness_factor);

    // Plane: y=x (normal: y-x=0)
    if (abs(localRayDir.y - localRayDir.x) > 0.0001) {
        float t = (localRayStart.x - localRayStart.y) / (localRayDir.y - localRayDir.x);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    // Plane: y=-x (normal: y+x=0)
    if (abs(localRayDir.y + localRayDir.x) > 0.0001) {
        float t = -(localRayStart.x + localRayStart.y) / (localRayDir.y + localRayDir.x);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    // Plane: y=z (normal: y-z=0)
    if (abs(localRayDir.y - localRayDir.z) > 0.0001) {
        float t = (localRayStart.z - localRayStart.y) / (localRayDir.y - localRayDir.z);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    // Plane: y=-z (normal: y+z=0)
    if (abs(localRayDir.y + localRayDir.z) > 0.0001) {
        float t = -(localRayStart.z + localRayStart.y) / (localRayDir.y + localRayDir.z);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    // Plane: x=z (normal: x-z=0)
    if (abs(localRayDir.x - localRayDir.z) > 0.0001) {
        float t = (localRayStart.z - localRayStart.x) / (localRayDir.x - localRayDir.z);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    // Plane: x=-z (normal: x+z=0)
    if (abs(localRayDir.x + localRayDir.z) > 0.0001) {
        float t = -(localRayStart.z + localRayStart.x) / (localRayDir.x + localRayDir.z);
        if (t >= 0.0 && t <= thickness) {
            vec3 intersection = localRayStart + localRayDir * t;
            float dist = smoothMax3(vec3(abs(intersection.x), abs(intersection.y), abs(intersection.z)), roundness_factor);
            minDist = min(minDist, dist);
        }
    }

    float distanceFromCenter = minDist / v_half_size;

    float alpha;

    if(distanceFromCenter < 1.0) {
        alpha = (1. - dot(normalize(localIntPos),-localRayDir));
        alpha *= alpha * 0.6;
    } else {
        alpha = clamp(1. - (distanceFromCenter - 1.) / (AtmosphereThickness - 1.), 0.0, 1.0);
        alpha *= alpha;
    }

    vec3 hPos1 = localEnterPos / v_half_size;
    vec3 hPos2 = localIntPos / v_half_size;

    float lightDot = (dot(hPos1,-localLightDir) + dot(hPos2,-localLightDir)) / 2. / length(hPos1 - hPos2);

    float dayFact = clamp(lightDot * 3.0,0.0,1.0);
    float nightFact = clamp(lightDot * -4.0,0.0,1.0);

    frag_color = mix(mix(sunsetCol,nightCol,nightFact),dayCol,dayFact);
    frag_color.a *= alpha;
    frag_color.a *= Density;

    //frag_color = vec4(localRayDir * 0.5 + 0.5,0.3);

    //frag_color = vec4((v_entry_position + v_half_size) / (v_half_size * 2.0),0.5);
}