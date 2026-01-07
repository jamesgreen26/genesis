package shipwrights.genesis.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.Map;

public abstract class MiasmaFluid extends ForgeFlowingFluid {
    protected MiasmaFluid(Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getFlow(BlockGetter blockGetter, BlockPos pos, FluidState fluidState) {
        // Return zero flow - no pushing entities
        return Vec3.ZERO;
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        // Prevent spreading downward - gas doesn't fall
        if (direction == Direction.DOWN) {
            return;
        }
        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    @Override
    protected Map<Direction, FluidState> getSpread(Level level, BlockPos pos, BlockState state) {
        Map<Direction, FluidState> spreadMap = super.getSpread(level, pos, state);
        // Remove downward spread
        spreadMap.remove(Direction.DOWN);
        return spreadMap;
    }

    @Override
    protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluid) {
        // Prevent flowing downward - gas rises, doesn't fall
        if (direction == Direction.DOWN) {
            return false;
        }
        return super.canSpreadTo(level, fromPos, fromBlockState, direction, toPos, toBlockState, toFluidState, fluid);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        // How far to search for a path sideways
        return 4;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        // Get the fluid level (1-8, where 8 is full source)
        int amount = state.getAmount();

        // Always spawn at least one particle
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble() * 0.5 + 0.2;
        double z = pos.getZ() + random.nextDouble();

        // Spawn magenta witch particles rising upward
        level.addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x, y, z,
                (random.nextDouble() - 0.5) * 0.05,
                0.02 + random.nextDouble() * 0.03,
                (random.nextDouble() - 0.5) * 0.05
        );

        // More particles for higher fluid levels
        if (random.nextInt(8) < amount) {
            level.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble() * 0.5 + 0.2,
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.05,
                    0.03 + random.nextDouble() * 0.04,
                    (random.nextDouble() - 0.5) * 0.05
            );
        }
    }

    public static class Source extends MiasmaFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends MiasmaFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
