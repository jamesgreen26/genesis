package shipwrights.dataplanets.items;

import shipwrights.dataplanets.factions.Questable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TaskListItem extends Item
{
    public TaskListItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_41432_, Player p_41433_, InteractionHand p_41434_) {
        if(p_41433_ instanceof ServerPlayer spe)
        {
            Questable.interpret(spe);
        }
        return super.use(p_41432_, p_41433_, p_41434_);
    }
}
