package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.items.SpaceArmourItem;
import shipwrights.dataplanets.items.*;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class DPItems {

    static {
        Reg.REGISTRATE.defaultCreativeTab("dataplanets").register();
    }

    public static ItemEntry<TheoryItem> THEORY = Reg.REGISTRATE.item("theory", a->new TheoryItem(a,1)).properties((a)->a).lang("Visible Theory").register();
    public static ItemEntry<PortableTelescopeItem> PORTABLE_TELESCOPE = Reg.REGISTRATE.item("portable_telescope", PortableTelescopeItem::new).properties((a)->a).register();
    public static ItemEntry<ResearchItem> RESEARCH = Reg.REGISTRATE.item("research", ResearchItem::new).properties((a)->a).register();
    public static ItemEntry<TestItem> TEST_ITEM = Reg.REGISTRATE.item("test_item",TestItem::new).properties((a)->a).lang("Test Item").register();
    public static ItemEntry<TaskListItem> TASK_LIST = Reg.REGISTRATE.item("task_list",TaskListItem::new).properties((a)->a).lang("Task List").register();
    public static ItemEntry<Item> NEUM_TRACKS = Reg.REGISTRATE.item("neum_tracks",Item::new).properties((a)->a).lang("Neum Tracks").register();
    public static ItemEntry<Item> NEUM_HEAD = Reg.REGISTRATE.item("neum_head",Item::new).properties((a)->a).lang("Neum Head").register();
    public static ItemEntry<SpaceHelmetItem> SPACE_HELMET = Reg.REGISTRATE.item("breathing_helmet",SpaceHelmetItem::new).properties((a)->a).lang("Space Helmet").register();

    public static final SpaceSuitArmourMaterial SPACE_SUIT_ARMOUR = new SpaceSuitArmourMaterial();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_HELMET = Reg.REGISTRATE.item("space_helmet", a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.HELMET,a)).properties((a)->a).lang("Space Helmet").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_CHEST = Reg.REGISTRATE.item("space_chestplate",a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.CHESTPLATE,a)).properties((a)->a).lang("Space Chestplate").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_LEGS = Reg.REGISTRATE.item("space_leggings",a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.LEGGINGS,a)).properties((a)->a).lang("Space Leggings").register();
    public static ItemEntry<SpaceArmourItem> SPACE_SUIT_BOOTS = Reg.REGISTRATE.item("space_boots", a->new SpaceArmourItem(SPACE_SUIT_ARMOUR, SpaceArmourItem.Type.BOOTS,a)).properties((a)->a).lang("Space Boots").register();


    public static void init() {}
}
