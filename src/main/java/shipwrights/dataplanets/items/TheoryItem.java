package shipwrights.dataplanets.items;

import shipwrights.dataplanets.space.DynamicSystems;
import shipwrights.dataplanets.space.StarSystemCreator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TheoryItem extends Item {
    private static final int MAX_LEVEL = 2;
    public int level;
    public TheoryItem(Properties properties,int level) {
        super(properties);
        this.level=level;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand usedHand) {
        if(!world.isClientSide && usedHand==InteractionHand.MAIN_HAND)
        {
            boolean didPass = false;
            for (int i = 0; i < level; i++)
            {
                if(world.random.nextInt(MAX_LEVEL)==0)
                {
                    StarSystemCreator.makeSystem(0, 6);
                    didPass=true;
                }
            }

            if(didPass)
            {

                player.sendSystemMessage(Component.empty()
                        .append("You theory was successfully proven, new system(s) are confirmed to exist!").copy()
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));

                DynamicSystems.onGenSetup(world.getServer());
            }
            else
            {
                player.sendSystemMessage(Component.empty().append("Unfortunately, your theory was found to be incorrect, try again next time!"));
            }
            player.getItemInHand(usedHand).shrink(1);

        }
        return super.use(world, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
        p_41423_.add(Component.empty().append("Theory tier ").append(this.level+"/"+MAX_LEVEL));
        p_41423_.add(Component.empty().append("Use this item for a chance to discover a star system, higher tiers have better chances!"));
    }
}
