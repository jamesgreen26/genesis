package shipwrights.genesis.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/// A thin layer of ice that cracks when something steps on it, such as over the crevasses in the moon's ice caverns.
/// The cracks spread through each {@link #AGE} over a couple of seconds, then it breaks. Only drops with Silk Touch
/// (see its loot table).
public class BrittleIceBlock extends HalfTransparentBlock {
    /// How far it has cracked, from 0 for whole to {@link #MAX_AGE}
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final int MAX_AGE = 3;

    /// Time between each stage of cracking, and from the last to breaking: two seconds in all
    private static final int CRACK_DELAY_TICKS = 10;

    public BrittleIceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, CRACK_DELAY_TICKS);
            crackSound(level, pos);
        }
        super.stepOn(level, pos, state, entity);
    }

    /// Once cracking has started, it carries on whether or not anything is still standing on it
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, CRACK_DELAY_TICKS);
            crackSound(level, pos);
        } else {
            level.destroyBlock(pos, false);
        }
    }

    private static void crackSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 1.0F, 0.6F);
    }
}
