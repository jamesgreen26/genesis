package g_mungus.genesis.asteroid.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import g_mungus.genesis.mixin.BSDVSAccessor;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;

import java.io.Serializable;
import java.util.BitSet;

public class BitDiscreteVoxelShapeWrapper implements Serializable {

    private final BitSet storage;
    private final int xMin;
    private final int yMin;
    private final int zMin;
    private final int xMax;
    private final int yMax;

    public BitSet getStorage() {
        return storage;
    }

    public int getZMax() {
        return zMax;
    }

    public int getYMax() {
        return yMax;
    }

    public int getXMax() {
        return xMax;
    }

    public int getZMin() {
        return zMin;
    }

    public int getYMin() {
        return yMin;
    }

    public int getXMin() {
        return xMin;
    }

    private final int zMax;

    public BitDiscreteVoxelShapeWrapper(BitSetDiscreteVoxelShape source) {
        BSDVSAccessor shape = ((BSDVSAccessor)(Object)source);
        assert shape != null;
        this.storage = shape.getStorage();
        this.xMin = shape.getXMin();
        this.yMin = shape.getYMin();
        this.zMin = shape.getZMin();
        this.xMax = shape.getXMax();
        this.yMax = shape.getYMax();
        this.zMax = shape.getZMax();
    }

    @JsonCreator
    public BitDiscreteVoxelShapeWrapper(
            @JsonProperty("storage") BitSet storage,
            @JsonProperty("xmin") int xMin,
            @JsonProperty("ymin") int yMin,
            @JsonProperty("zmin") int zMin,
            @JsonProperty("xmax") int xMax,
            @JsonProperty("ymax") int yMax,
            @JsonProperty("zmax") int zMax
    ) {
        this.storage = storage;
        this.xMin = xMin;
        this.yMin = yMin;
        this.zMin = zMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.zMax = zMax;
    }

    public BitSetDiscreteVoxelShape get() {
        BitSetDiscreteVoxelShape result = BitSetDiscreteVoxelShape.withFilledBounds(xMax - xMin, yMax - yMin, zMax - zMin, xMin, yMin, zMin, xMax, yMax, zMax);
        ((BSDVSAccessor)(Object)result).setStorage(storage);
        return result;
    }

    @Override
    public String toString() {
        return "BitSet3DWrapper{" +
                ", storage= " + storage +
                ", xMin=" + xMin +
                ", yMin=" + yMin +
                ", zMin=" + zMin +
                ", xMax=" + xMax +
                ", yMax=" + yMax +
                ", zMax=" + zMax +
                '}';
    }
}
