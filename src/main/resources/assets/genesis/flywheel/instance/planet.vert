void flw_instanceVertex(in FlwInstance instance) {
    flw_vertexColor = vec4(instance.localCameraPos.xyz, instance.halfSize);
    flw_vertexNormal = vec3(flw_vertexPos.xyz);
    flw_vertexPos = instance.pose * flw_vertexPos;
}
