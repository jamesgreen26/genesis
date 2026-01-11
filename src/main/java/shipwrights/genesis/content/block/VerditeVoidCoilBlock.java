package shipwrights.genesis.content.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class VerditeVoidCoilBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS;

    public VerditeVoidCoilBlock(BlockBehaviour.Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)this.defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    public BlockState rotate(BlockState arg, Rotation arg2) {
        return rotatePillar(arg, arg2);
    }

    public static BlockState rotatePillar(BlockState arg, Rotation arg2) {
        switch (arg2) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch ((Direction.Axis)arg.getValue(AXIS)) {
                    case X -> {
                        return (BlockState)arg.setValue(AXIS, Direction.Axis.Z);
                    }
                    case Z -> {
                        return (BlockState)arg.setValue(AXIS, Direction.Axis.X);
                    }
                    default -> {
                        return arg;
                    }
                }
            default:
                return arg;
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{AXIS});
    }

    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        return (BlockState)this.defaultBlockState().setValue(AXIS, arg.getClickedFace().getAxis());
    }

    static {
        AXIS = BlockStateProperties.AXIS;
    }
}
