package g_mungus.genesis.asteroid;

import g_mungus.genesis.asteroid.generation.AsteroidGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;

public class AsteroidBlock extends Block {
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, ASTEROID_COUNT);
    public static final List<VoxelShape> asteroidShapes = new ArrayList<>(ASTEROID_COUNT);


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
        int index = state.getValue(INDEX);

        try {
            return asteroidShapes.get(index);
        } catch (Exception e) {
            return Shapes.block();
        }
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter arg2, BlockPos arg3) {
        int index = state.getValue(INDEX);

        try {
            return asteroidShapes.get(index);
        } catch (Exception e) {
            return Shapes.block();
        }
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
