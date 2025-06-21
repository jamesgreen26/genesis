package g_mungus.genesis.mixin;


import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;

@Mixin(BitSetDiscreteVoxelShape.class)
public interface BSDVSAccessor {

    @Accessor("storage")
    BitSet getStorage();

    @Accessor("xMin")
    int getXMin();

    @Accessor("xMin")
    void setXMin(int xMin);

    @Accessor("yMin")
    int getYMin();

    @Accessor("yMin")
    void setYMin(int yMin);

    @Accessor("zMin")
    int getZMin();

    @Accessor("zMin")
    void setZMin(int zMin);

    @Accessor("xMax")
    int getXMax();

    @Accessor("xMax")
    void setXMax(int xMax);

    @Accessor("yMax")
    int getYMax();

    @Accessor("yMax")
    void setYMax(int yMax);

    @Accessor("zMax")
    int getZMax();

    @Accessor("zMax")
    void setZMax(int zMax);
}
