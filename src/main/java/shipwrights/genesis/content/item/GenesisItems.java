package shipwrights.genesis.content.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.block.GenesisBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class GenesisItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, GenesisMod.MOD_ID);

    public static Map<String, RegistryObject<Item>> DYNAMIC_ITEMS = new HashMap<>();

    public static final RegistryObject<Item> NAV_PROJECTOR = ITEMS.register("nav_projector",
        () -> new BlockItem(GenesisBlocks.NAV_PROJECTOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> RADAR_DISPLAY = ITEMS.register("radar_display",
        () -> new BlockItem(GenesisBlocks.RADAR_DISPLAY.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_ENGINE_INTERFACE = ITEMS.register("void_engine_interface",
        () -> new BlockItem(GenesisBlocks.VOID_ENGINE_INTERFACE.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_ENGINE_FRAME = ITEMS.register("void_engine_frame",
        () -> new BlockItem(GenesisBlocks.VOID_ENGINE_FRAME.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_CORE = ITEMS.register("void_core",
        () -> new BlockItem(GenesisBlocks.VOID_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_ENGINE_VIEWPORT = ITEMS.register("void_engine_viewport",
        () -> new BlockItem(GenesisBlocks.VOID_ENGINE_VIEWPORT.get(), new Item.Properties()));

    // Alien stones
    public static final RegistryObject<Item> VOIDSTONE = ITEMS.register("voidstone",
        () -> new BlockItem(GenesisBlocks.VOIDSTONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RIFTROCK = ITEMS.register("riftrock",
        () -> new BlockItem(GenesisBlocks.RIFTROCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> NULLSTONE = ITEMS.register("nullstone",
        () -> new BlockItem(GenesisBlocks.NULLSTONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ECHOSTONE = ITEMS.register("echostone",
        () -> new BlockItem(GenesisBlocks.ECHOSTONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> PHASEROCK = ITEMS.register("phaserock",
        () -> new BlockItem(GenesisBlocks.PHASEROCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WARPSTONE = ITEMS.register("warpstone",
        () -> new BlockItem(GenesisBlocks.WARPSTONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TULCITE_ORE = ITEMS.register("tulcite_ore",
            () -> new BlockItem(GenesisBlocks.TULCITE_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> VERDITE_ORE = ITEMS.register("verdite_ore",
        () -> new BlockItem(GenesisBlocks.VERDITE_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_CORE_ORE = ITEMS.register("void_core_ore",
            () -> new BlockItem(GenesisBlocks.VOID_CORE_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ANORTHITE = ITEMS.register("anorthite",
            () -> new BlockItem(GenesisBlocks.ANORTHITE.get(), new Item.Properties()));

    // Alien sands
    public static final RegistryObject<Item> LUNAR_DUST = ITEMS.register("lunar_dust",
        () -> new BlockItem(GenesisBlocks.LUNAR_DUST.get(), new Item.Properties()));

    public static final RegistryObject<Item> STELLAR_SAND = ITEMS.register("stellar_sand",
        () -> new BlockItem(GenesisBlocks.STELLAR_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> PALE_RED_SALT = ITEMS.register("pale_red_salt",
            () -> new BlockItem(GenesisBlocks.PALE_RED_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> RED_SALT = ITEMS.register("red_salt",
            () -> new BlockItem(GenesisBlocks.RED_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CRACKED_PALE_RED_SALT = ITEMS.register("cracked_pale_red_salt",
            () -> new BlockItem(GenesisBlocks.CRACKED_PALE_RED_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CRACKED_RED_SALT = ITEMS.register("cracked_red_salt",
            () -> new BlockItem(GenesisBlocks.CRACKED_RED_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CYAN_SALT = ITEMS.register("cyan_salt",
            () -> new BlockItem(GenesisBlocks.CYAN_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> TURQUOISE_SALT = ITEMS.register("turquoise_salt",
            () -> new BlockItem(GenesisBlocks.TURQUOISE_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CRACKED_CYAN_SALT = ITEMS.register("cracked_cyan_salt",
            () -> new BlockItem(GenesisBlocks.CRACKED_CYAN_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CRACKED_TURQUOISE_SALT = ITEMS.register("cracked_turquoise_salt",
            () -> new BlockItem(GenesisBlocks.CRACKED_TURQUOISE_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> SALT = ITEMS.register("salt",
            () -> new BlockItem(GenesisBlocks.SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CRACKED_SALT = ITEMS.register("cracked_salt",
            () -> new BlockItem(GenesisBlocks.CRACKED_SALT.get(), new Item.Properties()));

    public static final RegistryObject<Item> MOON_STONE = ITEMS.register("moon_stone",
            () -> new BlockItem(GenesisBlocks.MOON_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> HALLOW_MOON_STONE = ITEMS.register("hallow_moon_stone",
            () -> new BlockItem(GenesisBlocks.HALLOW_MOON_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MOON_SAND = ITEMS.register("moon_sand",
            () -> new BlockItem(GenesisBlocks.MOON_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> WAVY_MOON_SAND = ITEMS.register("wavy_moon_sand",
            () -> new BlockItem(GenesisBlocks.WAVY_MOON_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> DARK_MOON_SAND = ITEMS.register("dark_moon_sand",
            () -> new BlockItem(GenesisBlocks.DARK_MOON_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> DARK_WAVY_MOON_SAND = ITEMS.register("dark_wavy_moon_sand",
            () -> new BlockItem(GenesisBlocks.DARK_WAVY_MOON_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEAD_MOON_CORAL_BLOCK = ITEMS.register("dead_moon_coral_block",
            () -> new BlockItem(GenesisBlocks.DEAD_MOON_CORAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEAD_MOON_CORAL = ITEMS.register("dead_moon_coral",
            () -> new BlockItem(GenesisBlocks.DEAD_MOON_CORAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEAD_MOON_CORAL_FAN = ITEMS.register("dead_moon_coral_fan",
            () -> new StandingAndWallBlockItem(GenesisBlocks.DEAD_MOON_CORAL_FAN.get(), GenesisBlocks.DEAD_MOON_CORAL_WALL_FAN.get(), new Item.Properties(), Direction.DOWN));

    public static final RegistryObject<Item> BRINE_TRUNK = ITEMS.register("brine_trunk",
            () -> new BlockItem(GenesisBlocks.BRINE_TRUNK.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRINE_FLOWER = ITEMS.register("brine_flower",
            () -> new BlockItem(GenesisBlocks.BRINE_FLOWER.get(), new Item.Properties()));

    public static final RegistryObject<Item> PETRIFIED_BUSH = ITEMS.register("petrified_bush",
            () -> new BlockItem(GenesisBlocks.PETRIFIED_BUSH.get(), new Item.Properties()));

    public static final RegistryObject<Item> ZAPLIGHT_SPINDLES = ITEMS.register("zap_light_spindles",
            () -> new StandingAndWallBlockItem(GenesisBlocks.ZAPLIGHT_SPINDLES.get(), GenesisBlocks.WALL_ZAPLIGHT_SPINDLES.get(), new Item.Properties(), Direction.DOWN));

    public static final RegistryObject<Item> ZAPLIGHT = ITEMS.register("zaplight",
            () -> new BlockItem(GenesisBlocks.ZAPLIGHT.get(), new Item.Properties()));

    public static final RegistryObject<Item> BUBBLE_SHROOM_STALK = ITEMS.register("bubble_shroom_stalk",
            () -> new BlockItem(GenesisBlocks.BUBBLE_SHROOM_STALK.get(), new Item.Properties()));

    public static final RegistryObject<Item> BUBBLE_SHROOM_CAP = ITEMS.register("bubble_shroom_cap",
            () -> new BlockItem(GenesisBlocks.BUBBLE_SHROOM_CAP.get(), new Item.Properties()));

    public static final RegistryObject<Item> BEARD_SHROOM_CAP = ITEMS.register("beard_shroom_cap",
            () -> new BlockItem(GenesisBlocks.BEARD_SHROOM_CAP.get(), new Item.Properties()));

    public static final RegistryObject<Item> BEARD_SHROOM_LOG = ITEMS.register("beard_shroom_log",
            () -> new BlockItem(GenesisBlocks.BEARD_SHROOM_LOG.get(), new Item.Properties()));

    public static final RegistryObject<Item> BEARD_SHROOM_LEAVES = ITEMS.register("beard_shroom_leaves",
            () -> new BlockItem(GenesisBlocks.BEARD_SHROOM_LEAVES.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLOWERING_BEARD_SHROOM_LEAVES = ITEMS.register("flowering_beard_shroom_leaves",
            () -> new BlockItem(GenesisBlocks.FLOWERING_BEARD_SHROOM_LEAVES.get(), new Item.Properties()));

    public static final RegistryObject<Item> HANGING_BEARD_SHROOM_LEAVES = ITEMS.register("hanging_beard_shroom_leaves",
            () -> new BlockItem(GenesisBlocks.HANGING_BEARD_SHROOM_LEAVES.get(), new Item.Properties()));



    public static final RegistryObject<Item> AZURE_MOSS = ITEMS.register("azure_moss",
            () -> new BlockItem(GenesisBlocks.AZURE_MOSS.get(), new Item.Properties()));

    public static final RegistryObject<Item> CYAN_MOSS = ITEMS.register("cyan_moss",
            () -> new BlockItem(GenesisBlocks.CYAN_MOSS.get(), new Item.Properties()));

    public static final RegistryObject<Item> TURQUOISE_MOSS = ITEMS.register("turquoise_moss",
            () -> new BlockItem(GenesisBlocks.TURQUOISE_MOSS.get(), new Item.Properties()));

    public static final RegistryObject<Item> AZURE_MOSS_CARPET = ITEMS.register("azure_moss_carpet",
            () -> new BlockItem(GenesisBlocks.AZURE_MOSS_CARPET.get(), new Item.Properties()));

    public static final RegistryObject<Item> CYAN_MOSS_CARPET = ITEMS.register("cyan_moss_carpet",
            () -> new BlockItem(GenesisBlocks.CYAN_MOSS_CARPET.get(), new Item.Properties()));

    public static final RegistryObject<Item> TURQUOISE_MOSS_CARPET = ITEMS.register("turquoise_moss_carpet",
            () -> new BlockItem(GenesisBlocks.TURQUOISE_MOSS_CARPET.get(), new Item.Properties()));


    public static final RegistryObject<Item> SPINDLE_GRASS = ITEMS.register("spindle_grass",
            () -> new BlockItem(GenesisBlocks.SPINDLE_GRASS.get(), new Item.Properties()));

    public static final RegistryObject<Item> TALL_SPINDLE_GRASS = ITEMS.register("tall_spindle_grass",
            () -> new BlockItem(GenesisBlocks.TALL_SPINDLE_GRASS.get(), new Item.Properties()));

    public static final RegistryObject<Item> SPINDLE_BUSH = ITEMS.register("spindle_bush",
            () -> new BlockItem(GenesisBlocks.SPINDLE_BUSH.get(), new Item.Properties()));

    public static final RegistryObject<Item> BELL_FLOWER = ITEMS.register("bell_flower",
            () -> new BlockItem(GenesisBlocks.BELL_FLOWER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SMALL_VERDITE_BUD = ITEMS.register("small_verdite_bud",
            () -> new BlockItem(GenesisBlocks.SMALL_VERDITE_BUD.get(), new Item.Properties()));

    public static final RegistryObject<Item> MEDIUM_VERDITE_BUD = ITEMS.register("medium_verdite_bud",
            () -> new BlockItem(GenesisBlocks.MEDIUM_VERDITE_BUD.get(), new Item.Properties()));

    public static final RegistryObject<Item> LARGE_VERDITE_BUD = ITEMS.register("large_verdite_bud",
            () -> new BlockItem(GenesisBlocks.LARGE_VERDITE_BUD.get(), new Item.Properties()));

    public static final RegistryObject<Item> VERDITE_CLUSTER = ITEMS.register("verdite_cluster",
            () -> new BlockItem(GenesisBlocks.VERDITE_CLUSTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> VERDITE_CRYSTAL_BLOCK = ITEMS.register("verdite_crystal_block",
            () -> new BlockItem(GenesisBlocks.VERDITE_CRYSTAL_BLOCK.get(), new Item.Properties()));


    public static final RegistryObject<Item> HEMATITE = ITEMS.register("hematite",
            () -> new BlockItem(GenesisBlocks.HEMATITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEMATITE_SAND = ITEMS.register("hematite_sand",
            () -> new BlockItem(GenesisBlocks.HEMATITE_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEMATITE_GRAVEL = ITEMS.register("hematite_gravel",
            () -> new BlockItem(GenesisBlocks.HEMATITE_GRAVEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEMATITE_SPROUTS = ITEMS.register("hematite_sprouts",
            () -> new BlockItem(GenesisBlocks.HEMATITE_SPROUTS.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEMATITE_BRAMBLE = ITEMS.register("hematite_bramble",
            () -> new BlockItem(GenesisBlocks.HEMATITE_BRAMBLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROOTED_SMOLDERING_HEMATITE = ITEMS.register("rooted_smoldering_hematite",
            () -> new BlockItem(GenesisBlocks.ROOTED_SMOLDERING_HEMATITE.get(), new Item.Properties()));


    public static final RegistryObject<Item> CHALCOPYRITE = ITEMS.register("chalcopyrite",
            () -> new BlockItem(GenesisBlocks.CHALCOPYRITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHALCOPYRITE_SAND = ITEMS.register("chalcopyrite_sand",
            () -> new BlockItem(GenesisBlocks.CHALCOPYRITE_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHALCOPYRITE_GRAVEL = ITEMS.register("chalcopyrite_gravel",
            () -> new BlockItem(GenesisBlocks.CHALCOPYRITE_GRAVEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHALCOPYRITE_SPROUTS = ITEMS.register("chalcopyrite_sprouts",
            () -> new BlockItem(GenesisBlocks.CHALCOPYRITE_SPROUTS.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHALCOPYRITE_BRAMBLE = ITEMS.register("chalcopyrite_bramble",
            () -> new BlockItem(GenesisBlocks.CHALCOPYRITE_BRAMBLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROOTED_SMOLDERING_CHALCOPYRITE = ITEMS.register("rooted_smoldering_chalcopyrite",
            () -> new BlockItem(GenesisBlocks.ROOTED_SMOLDERING_CHALCOPYRITE.get(), new Item.Properties()));


    public static final RegistryObject<Item> MALACHITE = ITEMS.register("malachite",
            () -> new BlockItem(GenesisBlocks.MALACHITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MALACHITE_SAND = ITEMS.register("malachite_sand",
            () -> new BlockItem(GenesisBlocks.MALACHITE_SAND.get(), new Item.Properties()));

    public static final RegistryObject<Item> MALACHITE_GRAVEL = ITEMS.register("malachite_gravel",
            () -> new BlockItem(GenesisBlocks.MALACHITE_GRAVEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> MALACHITE_SPROUTS = ITEMS.register("malachite_sprouts",
            () -> new BlockItem(GenesisBlocks.MALACHITE_SPROUTS.get(), new Item.Properties()));

    public static final RegistryObject<Item> MALACHITE_BRAMBLE = ITEMS.register("malachite_bramble",
            () -> new BlockItem(GenesisBlocks.MALACHITE_BRAMBLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROOTED_SMOLDERING_MALACHITE = ITEMS.register("rooted_smoldering_malachite",
            () -> new BlockItem(GenesisBlocks.ROOTED_SMOLDERING_MALACHITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAINBOW_CHALCOPYRITE_BLOCK = ITEMS.register("rainbow_chalcopyrite_block",
            () -> new BlockItem(GenesisBlocks.RAINBOW_CHALCOPYRITE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAINBOW_CHALCOPYRITE_CLUSTER = ITEMS.register("rainbow_chalcopyrite_cluster",
            () -> new BlockItem(GenesisBlocks.RAINBOW_CHALCOPYRITE_CLUSTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAINBOW_CHALCOPYRITE_BRICK = ITEMS.register("rainbow_chalcopyrite_brick",
            () -> new BlockItem(GenesisBlocks.RAINBOW_CHALCOPYRITE_BRICK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WITHERING_WILLOW_LOG = ITEMS.register("withering_willow_log",
            () -> new BlockItem(GenesisBlocks.WITHERING_WILLOW_LOG.get(), new Item.Properties()));

    public static final RegistryObject<Item> WITHERING_WILLOW_BRANCH = ITEMS.register("withering_willow_branch",
            () -> new BlockItem(GenesisBlocks.WITHERING_WILLOW_BRANCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> WITHERING_WILLOW_LEAVES = ITEMS.register("withering_willow_leaves",
            () -> new BlockItem(GenesisBlocks.WITHERING_WILLOW_LEAVES.get(), new Item.Properties()));

    public static final RegistryObject<Item> SMOLDERING_LILY = ITEMS.register("smoldering_lily",
            () -> new BlockItem(GenesisBlocks.SMOLDERING_LILY.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_CORE_REFLECTOR_PANEL = ITEMS.register("void_core_reflector_panel",
            () -> new BlockItem(GenesisBlocks.VOID_CORE_REFLECTOR_PANEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOID_FOCUS = ITEMS.register("void_focus",
            () -> new BlockItem(GenesisBlocks.VOID_FOCUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> VERDITE_VOID_COIL = ITEMS.register("verdite_void_coil",
            () -> new BlockItem(GenesisBlocks.VERDITE_VOID_COIL.get(), new Item.Properties()));


    public static final RegistryObject<Item> TULCITE_CHUNK = ITEMS.register("tulcite_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VOID_SHARD = ITEMS.register("void_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANORTHITE_CRYSTAL = ITEMS.register("anorthite_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TULCITE_CATALYZER_BLOCK_ITEM = ITEMS.register("tulcite_catalyzer_block",
        () -> new BlockItem(GenesisBlocks.TULCITE_CATALYZER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item",()->new TestItem(new Item.Properties()));

    public static final SpaceArmourMaterial SPACE_ARMOUR_MATERIAL = new SpaceArmourMaterial();
    public static final RegistryObject<Item> SPACE_HELMET = ITEMS.register("space_helmet", ()-> new SpaceArmourItem(SPACE_ARMOUR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SPACE_CHESTPLATE = ITEMS.register("space_chestplate", ()-> new SpaceArmourItem(SPACE_ARMOUR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SPACE_LEGGINGS = ITEMS.register("space_leggings", ()-> new SpaceArmourItem(SPACE_ARMOUR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SPACE_BOOTS = ITEMS.register("space_boots", ()-> new SpaceArmourItem(SPACE_ARMOUR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties()));

}
