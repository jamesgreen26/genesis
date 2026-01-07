package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class SmolderingRootBlock extends Block {
    public static final BooleanProperty LIT;

    public SmolderingRootBlock(Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(LIT, false));
    }

    public void attack(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
        interact(arg, arg2, arg3);
        super.attack(arg, arg2, arg3, arg4);
    }

    public void stepOn(Level arg, BlockPos arg2, BlockState arg3, Entity arg4) {
        if (!arg4.isSteppingCarefully()) {
            interact(arg3, arg, arg2);
        }

        super.stepOn(arg, arg2, arg3, arg4);
    }

    public InteractionResult use(BlockState arg, Level arg2, BlockPos arg3, Player arg4, InteractionHand arg5, BlockHitResult arg6) {
        if (arg2.isClientSide) {
            spawnParticles(arg2, arg3);
        } else {
            interact(arg, arg2, arg3);
        }

        ItemStack itemstack = arg4.getItemInHand(arg5);
        return itemstack.getItem() instanceof BlockItem && (new BlockPlaceContext(arg4, arg5, itemstack, arg6)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private static void interact(BlockState arg, Level arg2, BlockPos arg3) {
        spawnParticles(arg2, arg3);
        if (!(Boolean)arg.getValue(LIT)) {
            arg2.setBlock(arg3, (BlockState)arg.setValue(LIT, true), 3);
        }

    }

    public boolean isRandomlyTicking(BlockState arg) {
        return (Boolean)arg.getValue(LIT);
    }

    public void randomTick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        if ((Boolean)arg.getValue(LIT)) {
            arg2.setBlock(arg3, (BlockState)arg.setValue(LIT, false), 3);
        }

    }

    public void spawnAfterBreak(BlockState arg, ServerLevel arg2, BlockPos arg3, ItemStack arg4, boolean bl) {
        super.spawnAfterBreak(arg, arg2, arg3, arg4, bl);
    }

    public void animateTick(BlockState arg, Level arg2, BlockPos arg3, RandomSource arg4) {
        if ((Boolean)arg.getValue(LIT)) {
            spawnParticles(arg2, arg3);
        }

    }

    private static void spawnParticles(Level arg, BlockPos arg2) {
        double d0 = (double)0.5625F;
        RandomSource randomsource = arg.random;

        for(Direction direction : Direction.values()) {
            BlockPos blockpos = arg2.relative(direction);
            if (!arg.getBlockState(blockpos).isSolidRender(arg, blockpos)) {
                Direction.Axis direction$axis = direction.getAxis();
                double d1 = direction$axis == Direction.Axis.X ? (double)0.5F + (double)0.5625F * (double)direction.getStepX() : (double)randomsource.nextFloat();
                double d2 = direction$axis == Direction.Axis.Y ? (double)0.5F + (double)0.5625F * (double)direction.getStepY() : (double)randomsource.nextFloat();
                double d3 = direction$axis == Direction.Axis.Z ? (double)0.5F + (double)0.5625F * (double)direction.getStepZ() : (double)randomsource.nextFloat();
                arg.addParticle(ParticleTypes.SMOKE, (double)arg2.getX() + d1, (double)arg2.getY() + d2, (double)arg2.getZ() + d3, (double)0.0F, (double)0.0F, (double)0.0F);
            }
        }

    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{LIT});
    }

    static {
        LIT = RedstoneTorchBlock.LIT;
    }

}
