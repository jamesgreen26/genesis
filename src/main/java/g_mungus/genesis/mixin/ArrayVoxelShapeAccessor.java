package g_mungus.genesis.mixin;

import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArrayVoxelShape.class)
public interface ArrayVoxelShapeAccessor {

    @Accessor("xs")
    DoubleList getXs();

    @Accessor("ys")
    DoubleList getYs();

    @Accessor("zs")
    DoubleList getZs();

    @Invoker("<init>")
    static ArrayVoxelShape create(DiscreteVoxelShape shape, DoubleList xs, DoubleList ys, DoubleList zs) {
        throw new AssertionError(); // Mixin replaces this at runtime
    }
}
