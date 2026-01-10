package shipwrights.genesis.content.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpaceArmourItem extends ArmorItem {
    public SpaceArmourItem(ArmorMaterial arg, Type arg2, Properties arg3) {
        super(arg, arg2, arg3);
    }

    public static boolean hasModule(ItemStack armour,String moduleName)
    {
        if(armour.hasTag())
        {
            CompoundTag tag = armour.getTag();
            return tag.contains(moduleName);
        }
        return false;
    }

    public static void addModule(ItemStack armour, String moduleName)
    {
        CompoundTag tag = armour.getOrCreateTag();
        tag.putBoolean(moduleName,true);
        armour.setTag(tag);
    }

    @Override
    public void appendHoverText(ItemStack arg, @Nullable Level arg2, List<Component> list, TooltipFlag arg3) {
        if(arg.hasTag())
        {
            CompoundTag tag = arg.getTag();
            if(tag.contains("has_cooling"))
            {
                list.add(Component.literal("Has Cooling"));
            }
            if(tag.contains("has_heat"))
            {
                list.add(Component.literal("Has heat"));
            }
            if(tag.contains("has_oxygen"))
            {
                list.add(Component.literal("Has Oxygen"));
            }
        }
        super.appendHoverText(arg, arg2, list, arg3);
    }
}
