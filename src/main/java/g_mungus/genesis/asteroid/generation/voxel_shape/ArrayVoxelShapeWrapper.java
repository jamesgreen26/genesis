package g_mungus.genesis.asteroid.generation.voxel_shape;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import g_mungus.genesis.mixin.ArrayVoxelShapeAccessor;
import g_mungus.genesis.mixin.VoxelShapeAccessor;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ArrayVoxelShapeWrapper implements Serializable {

    private final List<Double> xs;
    private final List<Double> ys;
    private final List<Double> zs;

    private final BitSetDiscreteVoxelShapeWrapper shape;

    public ArrayVoxelShapeWrapper(ArrayVoxelShape source) {
        ArrayVoxelShapeAccessor accessor = (ArrayVoxelShapeAccessor) source;

        this.xs = accessor.getXs().toDoubleArray().length > 0 ?
                new ArrayList<>(DoubleArrayList.wrap(accessor.getXs().toDoubleArray())) : List.of();
        this.ys = accessor.getYs().toDoubleArray().length > 0 ?
                new ArrayList<>(DoubleArrayList.wrap(accessor.getYs().toDoubleArray())) : List.of();
        this.zs = accessor.getZs().toDoubleArray().length > 0 ?
                new ArrayList<>(DoubleArrayList.wrap(accessor.getZs().toDoubleArray())) : List.of();

        DiscreteVoxelShape discreteShape = ((VoxelShapeAccessor) source).getShape();

        if (discreteShape instanceof BitSetDiscreteVoxelShape bitSetShape) {
            this.shape = new BitSetDiscreteVoxelShapeWrapper(bitSetShape);
        } else {
            throw new IllegalArgumentException("Wrong type of shape");
        }
    }

    @JsonCreator
    public ArrayVoxelShapeWrapper(
            @JsonProperty("xs") List<Double> xs,
            @JsonProperty("ys") List<Double> ys,
            @JsonProperty("zs") List<Double> zs,
            @JsonProperty("shape") BitSetDiscreteVoxelShapeWrapper shape
    ) {
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.shape = shape;
    }

    public ArrayVoxelShape get() {

        DoubleList dxs = new DoubleArrayList(xs.stream().mapToDouble(Double::doubleValue).toArray());
        DoubleList dys = new DoubleArrayList(ys.stream().mapToDouble(Double::doubleValue).toArray());
        DoubleList dzs = new DoubleArrayList(zs.stream().mapToDouble(Double::doubleValue).toArray());

        return ArrayVoxelShapeAccessor.create(shape.get(), dxs, dys, dzs);
    }

    public List<Double> getXs() { return xs; }
    public List<Double> getYs() { return ys; }
    public List<Double> getZs() { return zs; }
    public BitSetDiscreteVoxelShapeWrapper getShape() { return shape; }
}

