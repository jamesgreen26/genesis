package shipwrights.genesis.space.renderer;

import dev.engine_room.flywheel.lib.math.DataPacker;
import dev.engine_room.flywheel.lib.vertex.AbstractVertexView;
import dev.engine_room.flywheel.lib.vertex.DefaultVertexList;
import org.lwjgl.system.MemoryUtil;

public class PosColorVertexView extends AbstractVertexView implements DefaultVertexList {
    public static final long STRIDE = 16L; // 3 floats for pos + 4 bytes for color (RGBA)

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

    // Color getters (normalized 0-1)
    public float r(int index) { return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 12)); }
    public float g(int index) { return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 13)); }
    public float b(int index) { return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 14)); }
    public float a(int index) { return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 15)); }

    // Color setters (normalized 0-1)
    public void r(int index, float r) { MemoryUtil.memPutByte(ptr + index * STRIDE + 12, DataPacker.packNormU8(r)); }
    public void g(int index, float g) { MemoryUtil.memPutByte(ptr + index * STRIDE + 13, DataPacker.packNormU8(g)); }
    public void b(int index, float b) { MemoryUtil.memPutByte(ptr + index * STRIDE + 14, DataPacker.packNormU8(b)); }
    public void a(int index, float a) { MemoryUtil.memPutByte(ptr + index * STRIDE + 15, DataPacker.packNormU8(a)); }
}
