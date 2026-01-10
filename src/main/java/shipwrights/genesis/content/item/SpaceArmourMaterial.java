package shipwrights.genesis.content.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class SpaceArmourMaterial implements ArmorMaterial {
    @Override
    public int getDurabilityForType(ArmorItem.Type arg) {
        return 200;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type arg) {
        return 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.WOOL_HIT;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.PHANTOM_MEMBRANE);
    }

    @Override
    public String getName() {
        return "genesis:space_suit";
    }

    @Override
    public float getToughness() {
        return 1;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }
}
