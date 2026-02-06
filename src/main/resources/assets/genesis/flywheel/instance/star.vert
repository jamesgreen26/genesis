void flw_instanceVertex(in FlwInstance instance) {
    flw_vertexPos = instance.pose * flw_vertexPos;
}
