float getHalfSize() {
    return flw_vertexColor.w;
}

vec3 getLocalPos() {
    return flw_vertexNormal.xyz * flw_vertexColor.w * 2.;
}


float smoothNormalize(float it) {
    return -log(1/2.718281828459 + pow(2.718281828459, -4 * (it + 0.11467)));
}

float smoothMax3(vec3 p, float roundness) {
    vec3 q = abs(p);
    float m = max(max(q.x, q.y), q.z);

    return clamp(m, 0, getHalfSize());
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

float rayBoxIntersection(vec3 rayStart, vec3 rayDir, float boxHalfSize, mat3 rotation) {
    mat3 invRotation = transpose(rotation);
    vec3 localRayStart = invRotation * rayStart;
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


void flw_materialFragment() {

    vec3 ray_direction = normalize(getLocalPos() - flw_vertexColor.xyz * 2.0);

    mat3 rot = rotationMatrix(vec3(0));

    float exit_distance = rayBoxIntersection(getLocalPos(), ray_direction, getHalfSize(), rot);

    float thickness = exit_distance;

    if (thickness < 0.0) {
        thickness = 0.0;
    }

    // Calculate minimum of distance components at intersections with 6 diagonal planes
    float minDist = 1e10;

    // Transform ray to local cube space
    mat3 invRot = transpose(rot);
    vec3 localRayStart = invRot * getLocalPos();
    vec3 localRayDir = invRot * ray_direction;

    // Check intersection with each of the 6 diagonal planes
    float roundness_factor = 0.5;

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

    float distanceFromCenter = minDist / getHalfSize();

    float brightness = max(0.0, 1.0 - distanceFromCenter);

    vec3 color = temperatureToColor(pow(0.5 * sin(3.1415 * (sqrt(2 * brightness + 0.25) - 1)) + 0.5, 0.3));


    flw_fragLight = vec2(1.0);
    flw_fragColor = vec4(color, pow(5 * brightness, 2));
//    flw_fragColor = vec4(getLocalPos(), 1);
}
