package shipwrights.genesis.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.WallTorchBlock;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.GenesisBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GenesisItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, GenesisMod.MOD_ID);

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

    public static final RegistryObject<Item> WARPSTONE_ORE = ITEMS.register("warpstone_ore",
        () -> new BlockItem(GenesisBlocks.WARPSTONE_ORE.get(), new Item.Properties()));

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



    public static final RegistryObject<Item> WARPSTONE_CHUNK = ITEMS.register("warpstone_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WARPSTONE_CATALYZER_BLOCK_ITEM = ITEMS.register("warpstone_catalyzer_block",
        () -> new BlockItem(GenesisBlocks.WARPSTONE_CATALYZER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item",()->new TestItem(new Item.Properties()));
}
