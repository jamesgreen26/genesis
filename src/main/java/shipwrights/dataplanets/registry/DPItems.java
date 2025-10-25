package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.items.SpaceArmourItem;
import shipwrights.dataplanets.items.*;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class DPItems {

    static {
        DataplanetsMod.REGISTRATE.defaultCreativeTab("dataplanets").register();
    }

    public static ItemEntry<TaskListItem> TASK_LIST = DataplanetsMod.REGISTRATE.item("task_list",TaskListItem::new).properties((a)->a).lang("Task List").register();
    public static ItemEntry<Item> NEUM_TRACKS = DataplanetsMod.REGISTRATE.item("neum_tracks",Item::new).properties((a)->a).lang("Neum Tracks").register();
    public static ItemEntry<Item> NEUM_HEAD = DataplanetsMod.REGISTRATE.item("neum_head",Item::new).properties((a)->a).lang("Neum Head").register();
    public static ItemEntry<SpaceHelmetItem> SPACE_HELMET = DataplanetsMod.REGISTRATE.item("breathing_helmet",SpaceHelmetItem::new).properties((a)->a).lang("Space Helmet").register();

    public static final SpaceSuitArmourMaterial SPACE_SUIT_ARMOUR = new SpaceSuitArmourMaterial();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_HELMET = DataplanetsMod.REGISTRATE.item("space_helmet", a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.HELMET,a)).properties((a)->a).lang("Space Helmet").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_CHEST = DataplanetsMod.REGISTRATE.item("space_chestplate",a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.CHESTPLATE,a)).properties((a)->a).lang("Space Chestplate").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_LEGS = DataplanetsMod.REGISTRATE.item("space_leggings",a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.LEGGINGS,a)).properties((a)->a).lang("Space Leggings").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_BOOTS = DataplanetsMod.REGISTRATE.item("space_boots", a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.BOOTS,a)).properties((a)->a).lang("Space Boots").register();

    /// do not delete
    public static void init() {}
}
