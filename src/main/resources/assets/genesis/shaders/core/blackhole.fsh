#version 150

in vec3 v_camera_pos;
in vec3 v_entry_position;
in float v_half_size;

out vec4 frag_color;

uniform float Opacity;

const vec3 v_cube_center = vec3(0);

const vec3 cube_rotationXYZ = vec3(0, 0, 0);

const vec4 accretionColor1 = vec4(1.0,1.0,0.2,1.0);
const vec4 accretionColor2 = vec4(1.0,0.5,0.1,0.9);
const vec4 accretionColor3 = vec4(1.0,0.3,0.0,0.6);

const float horizonRatio = 0.25;

float Max3(vec3 p) {
    vec3 q = abs(p);
    float m = max(max(q.x, q.y), q.z);

    return m;
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

vec4 accretionColor(vec2 p) {
    float t = max(abs(p.x),abs(p.y));
    t = (t - horizonRatio) / (1. - horizonRatio);
    t *= 3.0;
    if(t < 0.0) {
        return accretionColor1;
    } else if(t < 1.0) {
        return mix(accretionColor1,accretionColor2,t - 0.0);
    } else if(t < 2.0) {
        return mix(accretionColor2,accretionColor3,t - 1.0);
    } else if(t < 3.0) {
        return mix(accretionColor3,vec4(accretionColor3.xyz,0),t - 2.0);
    } else {
        return vec4(accretionColor3.xyz,0);
    }
}

void main() {
    vec3 rayDir = normalize(v_entry_position - v_camera_pos);
    vec3 rayPos = v_camera_pos - v_cube_center;

    mat3 rot = rotationMatrix(cube_rotationXYZ);
    mat3 invRot = transpose(rot);

    rayDir = invRot * rayDir;
    rayPos = (invRot * rayPos) / v_half_size;

    vec4 col1 = vec4(0);
    vec4 col2 = vec4(0);

    float accretionDistance = -rayPos.y / rayDir.y;

    float bendingStrength = Max3(rayPos);
    float bendingDistance = rayBoxIntersection(rayPos,rayDir,vec3(0),bendingStrength + 0.01,mat3(1,0,0,0,1,0,0,0,1));

    vec3 diagonalNormal;
    float diagonalDistance;
    float diagonalStrength;

    diagonalNormal = normalize(vec3(1,1,0));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    diagonalNormal = normalize(vec3(0,1,1));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    diagonalNormal = normalize(vec3(1,0,1));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    diagonalNormal = normalize(vec3(1,-1,0));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    diagonalNormal = normalize(vec3(0,1,-1));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    diagonalNormal = normalize(vec3(-1,0,1));
    diagonalDistance = -dot(rayPos,diagonalNormal) / dot(rayDir,diagonalNormal);
    if(diagonalDistance > 0.0) {
        diagonalStrength = Max3(rayPos + rayDir * diagonalDistance);
        if(diagonalStrength < bendingStrength) {
            bendingDistance = diagonalDistance;
            bendingStrength = diagonalStrength;
        }
    }

    float closestDistance = bendingDistance;
    bendingDistance = length(rayPos);

    if(accretionDistance > 0.0 && accretionDistance < bendingDistance) {
        col1 = accretionColor((rayPos + accretionDistance * rayDir).xz);
    }
    if(Max3(rayPos + accretionDistance * rayDir) < horizonRatio && accretionDistance > 0.0) {
        col1 = vec4(0,0,0,1);
    }

    float bendingFactor = pow(1. / (bendingStrength / horizonRatio),2.) * (1. - (bendingStrength - horizonRatio) / (1. - horizonRatio));
    rayPos = rayPos + rayDir * bendingDistance;
    rayDir = normalize(mix(rayDir,normalize(-rayPos),bendingFactor));
    float accretionPlaneDistance = accretionDistance;
    accretionDistance = -rayPos.y / rayDir.y;

    if(accretionDistance > 0.0) {
            col2 = accretionColor((rayPos + accretionDistance * rayDir).xz);
    }
    if(bendingFactor > 1.0) {
        col2 = vec4(0,0,0,1);
        if(closestDistance < accretionPlaneDistance) {
            col1 = vec4(0,0,0,1);
        }
    }

    frag_color = vec4(0);
    frag_color = mix(frag_color,vec4(col2.xyz,1),col2.a);
    frag_color = mix(frag_color,vec4(col1.xyz,1),col1.a);
    frag_color.a *= Opacity;
}