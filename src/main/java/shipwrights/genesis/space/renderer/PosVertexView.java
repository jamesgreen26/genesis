package shipwrights.genesis.space.renderer;

import dev.engine_room.flywheel.lib.vertex.AbstractVertexView;
import dev.engine_room.flywheel.lib.vertex.DefaultVertexList;
import org.lwjgl.system.MemoryUtil;

public class PosVertexView extends AbstractVertexView implements DefaultVertexList {
    public static final long STRIDE = 12L; // 3 floats for position

    @Override
    public long stride() {
        return STRIDE;
    }

    @Override
    public void vertexCount(int vertexCount) {
        super.vertexCount(vertexCount);
        // Allocate native memory for the vertices
        ptr = MemoryUtil.nmemAlloc(vertexCount * STRIDE);
    }

    // Position getters
    public float x(int index) { return MemoryUtil.memGetFloat(ptr + index * STRIDE); }
    public float y(int index) { return MemoryUtil.memGetFloat(ptr + index * STRIDE + 4); }
    public float z(int index) { return MemoryUtil.memGetFloat(ptr + index * STRIDE + 8); }

    // Position setters
    public void x(int index, float x) { MemoryUtil.memPutFloat(ptr + index * STRIDE, x); }
    public void y(int index, float y) { MemoryUtil.memPutFloat(ptr + index * STRIDE + 4, y); }
    public void z(int index, float z) { MemoryUtil.memPutFloat(ptr + index * STRIDE + 8, z); }
}