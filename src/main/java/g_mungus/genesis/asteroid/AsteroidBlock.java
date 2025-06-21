package g_mungus.genesis.asteroid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AsteroidBlock extends Block {
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, 255);


    VoxelShape shape = compileShape(AsteroidGenerator.generateAsteroid(0));

    public AsteroidBlock(Properties arg) {
        super(arg);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(INDEX, 0)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {

        return shape;
    }

    private VoxelShape compileShape(List<BlockPos> points) {
        return points.stream()
                .map(pos -> Block.box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))
                .reduce(Shapes::or)
                .orElse(Shapes.empty());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
