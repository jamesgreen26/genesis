package shipwrights.dataplanets.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

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
}
