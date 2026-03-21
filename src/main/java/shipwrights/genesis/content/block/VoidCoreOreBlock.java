package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import shipwrights.genesis.content.sound.GenesisSounds;

public class VoidCoreOreBlock extends DropExperienceBlock {
    int soundTicks = 0;

    public VoidCoreOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 1, false, false, false));
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 1, false, false, false));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (soundTicks == 47) {
            level.playLocalSound(pos, GenesisSounds.VOID_CORE_ORE_SOUND.get(), SoundSource.BLOCKS, 1.0F, 1.0F, true);
            soundTicks = 0;
        } else {
            soundTicks++;
        }
        //level.addParticle(ParticleTypes.ENCHANTED_HIT, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, 0.0f, 0.0f, 0.0f);
    }
}
