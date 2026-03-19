package shipwrights.genesis.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import shipwrights.genesis.content.sound.GenesisSounds;

import java.util.function.Supplier;

public class MiasmaLiquidBlock extends LiquidBlock {

    public MiasmaLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier, Properties properties) {
        super(fluidSupplier, properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 30);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide) {
            // Schedule a tick to check dissipation shortly after placement
            level.scheduleTick(pos, this, 30);
        }
    }

    @Override
    public void randomTick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        super.randomTick(arg, arg2, arg3, arg4);
        arg2.scheduleTick(arg3, this, 30);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        tryDissipate(level, pos);
    }

    private void tryDissipate(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        if (level.getBlockState(abovePos).isAir() && level.getFluidState(abovePos).isEmpty()) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, GenesisSounds.MIASMA_HISS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            // Dissipate - replace this fluid block with air
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            // Apply poison effect - 10 seconds (200 ticks), amplifier 1 (Poison II)
            if (entity instanceof Player player && player.isCreative()) {return;}
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1, false, true));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, true));
            livingEntity.hurt(level.damageSources().genericKill(), 2);
        }
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
