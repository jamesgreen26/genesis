package g_mungus.genesis.asteroid.generation;

import g_mungus.genesis.mixin.BSDVSAccessor;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;

import java.io.Serial;
import java.io.Serializable;

public class BitDiscreteVoxelShapeWrapper implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int xMin;
    private final int yMin;
    private final int zMin;
    private final int xMax;
    private final int yMax;
    private final int zMax;

    public BitDiscreteVoxelShapeWrapper(BitSetDiscreteVoxelShape source) {
        BSDVSAccessor shape = ((BSDVSAccessor)(Object)source);
        assert shape != null;
        this.xMin = shape.getXMin();
        this.yMin = shape.getYMin();
        this.zMin = shape.getZMin();
        this.xMax = shape.getXMax();
        this.yMax = shape.getYMax();
        this.zMax = shape.getZMax();
    }

    public BitSetDiscreteVoxelShape get() {
        return BitSetDiscreteVoxelShape.withFilledBounds(xMax - xMin, yMax - yMin, zMax - zMin, xMin, yMin, zMin, xMax, yMax, zMax);
    }

    @Override
    public String toString() {
        return "BitSet3DWrapper{" +
                ", xMin=" + xMin +
                ", yMin=" + yMin +
                ", zMin=" + zMin +
                ", xMax=" + xMax +
                ", yMax=" + yMax +
                ", zMax=" + zMax +
                '}';
    }
}
