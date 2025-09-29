package shipwrights.dataplanets.items;

import shipwrights.dataplanets.registry.DPItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortableTelescopeItem extends Item {
    public PortableTelescopeItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(!level.isClientSide)
        {
            if(player.getItemInHand(InteractionHand.MAIN_HAND).is(this)
                    && level.canSeeSky(player.getOnPos().above())
                    && level.getDayTime()>13000
                    && player.getItemInHand(InteractionHand.OFF_HAND).is(Items.PAPER))
            {
                String biome = level.getBiome(player.getOnPos()).unwrapKey().get().location().toString();
                int sectorX = player.getOnPos().getX()/1000;
                int sectorZ = player.getOnPos().getZ()/1000;
                player.sendSystemMessage(Component.empty().append("Biome: ").append(biome)
                        .append("\nSector: "+sectorX+" "+sectorZ).append("\nTime: ").append(""+level.getGameTime()));

                ItemStack stack = new ItemStack(DPItems.RESEARCH);
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString("biome",biome);
                tag.putInt("sectorX",sectorX);
                tag.putInt("sectorZ",sectorZ);
                tag.putLong("time",level.getGameTime());
                stack.setHoverName(Component.empty().withStyle(ChatFormatting.AQUA).append("Observation"));

                stack.setTag(tag);
                player.addItem(stack);
                player.getItemInHand(InteractionHand.OFF_HAND).shrink(1);
            }

        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
        p_41423_.add(Component.empty().append("Needs to be night time and able to see the sky to work!"));
        p_41423_.add(Component.empty().append("Use with paper in your off hand to make an observation in the current biome and sector"));
        p_41423_.add(Component.empty().append("(A sector is any 1000x1000 block area starting from 0,0 in any x or z direction)"));
    }
}
