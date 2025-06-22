package g_mungus.genesis.asteroid;

import g_mungus.genesis.asteroid.generation.AsteroidGenerator;
import g_mungus.genesis.asteroid.generation.composition.CompositionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;

public class AsteroidBlock extends Block {
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, ASTEROID_COUNT);
    public static final ConcurrentMap<Integer, VoxelShape> asteroidShapes = new ConcurrentHashMap<>();
    public static final ConcurrentMap<Integer, CompositionData> compositions = new ConcurrentHashMap<>();


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

        return asteroidShapes.getOrDefault(index, Shapes.block());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }



    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder arg2) {
        int index = state.getValue(INDEX);

        CompositionData comp = compositions.getOrDefault(index, new CompositionData());

        return comp.getComposition().stream().map(it -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(it.getType())), it.getAmount())).toList();
    }
}
