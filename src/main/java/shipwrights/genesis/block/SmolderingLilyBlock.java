package shipwrights.genesis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import shipwrights.genesis.particle.GenesisParticles;

public class SmolderingLilyBlock extends BaseCoralPlantBlock {
    public SmolderingLilyBlock(Properties arg) {
        super(arg);
    }

    public void animateTick(BlockState arg, Level arg2, BlockPos arg3, RandomSource arg4) {
        int i = arg3.getX();
        int j = arg3.getY();
        int k = arg3.getZ();
        double d = (double)i + arg4.nextDouble();
        double e = (double)j + arg4.nextDouble();
        double f = (double)k + arg4.nextDouble();

        if (arg4.nextInt(1) == 0) {
            arg2.addParticle(ParticleTypes.SMOKE, d, e, f, (double) 0.0F, (double) 0.0F, (double) 0.0F);
        }
    }
}
