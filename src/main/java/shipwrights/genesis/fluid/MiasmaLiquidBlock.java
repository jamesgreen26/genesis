package shipwrights.genesis.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Supplier;

public class MiasmaLiquidBlock extends LiquidBlock {

    public MiasmaLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier, Properties properties) {
        super(fluidSupplier, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Get the fluid level (0-15 for block state, but fluid is 1-8)
        int fluidLevel = state.getValue(LEVEL);
        // LEVEL 0 = source (full), higher = less fluid
        int amount = 8 - Math.min(fluidLevel, 7);

        // Always spawn at least one particle
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble() * 0.5 + 0.2;
        double z = pos.getZ() + random.nextDouble();

        // Spawn magenta witch particles rising upward
        level.addParticle(
                ParticleTypes.WITCH,
                x, y, z,
                (random.nextDouble() - 0.5) * 0.05,
                0.02 + random.nextDouble() * 0.03,
                (random.nextDouble() - 0.5) * 0.05
        );

        // More particles for higher fluid levels (source blocks have more)
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
}
