package g_mungus.genesis.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import open_simplex_2.java.OpenSimplex2;

import java.util.Random;

public class TestingStickItem extends Item {

    public TestingStickItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();


        return InteractionResult.SUCCESS;
    }


}
