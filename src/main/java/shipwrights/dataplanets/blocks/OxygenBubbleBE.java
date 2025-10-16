package shipwrights.dataplanets.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class OxygenBubbleBE extends BlockEntity {


    public OxygenBubbleBE(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, OxygenBubbleBE be)
    {
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,new AABB(pos.offset(-5,-5,-5),pos.offset(5,5,5)));
        for(LivingEntity entity: entities)
        {
            entity.addTag("in_oxygen_bubble"); 
        }

    }
}
