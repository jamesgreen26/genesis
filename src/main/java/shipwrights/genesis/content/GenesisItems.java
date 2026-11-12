package shipwrights.genesis.content;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import shipwrights.genesis.GenesisMod;

public final class GenesisItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GenesisMod.MOD_ID);

    // The moon

    public static final DeferredItem<Item> MOON_STONE = blockItem("moon_stone", GenesisBlocks.MOON_STONE);
    public static final DeferredItem<Item> MOON_STONE_BRICKS = blockItem("moon_stone_bricks", GenesisBlocks.MOON_STONE_BRICKS);
    public static final DeferredItem<Item> MOON_STONE_BRICK_SLAB = blockItem("moon_stone_brick_slab", GenesisBlocks.MOON_STONE_BRICK_SLAB);
    public static final DeferredItem<Item> MOON_STONE_BRICK_STAIRS = blockItem("moon_stone_brick_stairs", GenesisBlocks.MOON_STONE_BRICK_STAIRS);
    public static final DeferredItem<Item> MOON_STONE_BRICK_WALL = blockItem("moon_stone_brick_wall", GenesisBlocks.MOON_STONE_BRICK_WALL);
    public static final DeferredItem<Item> HALLOW_MOON_STONE = blockItem("hallow_moon_stone", GenesisBlocks.HALLOW_MOON_STONE);
    public static final DeferredItem<Item> MOON_IRON_ORE = blockItem("moon_iron_ore", GenesisBlocks.MOON_IRON_ORE);
    public static final DeferredItem<Item> MOON_REDSTONE_ORE = blockItem("moon_redstone_ore", GenesisBlocks.MOON_REDSTONE_ORE);
    public static final DeferredItem<Item> MOON_LITHIUM_ORE = blockItem("moon_lithium_ore", GenesisBlocks.MOON_LITHIUM_ORE);
    public static final DeferredItem<Item> ANORTHITE = blockItem("anorthite", GenesisBlocks.ANORTHITE);
    public static final DeferredItem<Item> ANORTHITE_CRYSTAL = ITEMS.register("anorthite_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRYSTALLINE_BASALT = blockItem("crystalline_basalt", GenesisBlocks.CRYSTALLINE_BASALT);
    public static final DeferredItem<Item> ZIRCON = ITEMS.register("zircon", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOON_SAND = blockItem("moon_sand", GenesisBlocks.MOON_SAND);
    public static final DeferredItem<Item> WAVY_MOON_SAND = blockItem("wavy_moon_sand", GenesisBlocks.WAVY_MOON_SAND);
    public static final DeferredItem<Item> DARK_MOON_SAND = blockItem("dark_moon_sand", GenesisBlocks.DARK_MOON_SAND);
    public static final DeferredItem<Item> DARK_WAVY_MOON_SAND = blockItem("dark_wavy_moon_sand", GenesisBlocks.DARK_WAVY_MOON_SAND);
    public static final DeferredItem<Item> SUSPICIOUS_DARK_MOON_SAND = blockItem("suspicious_dark_moon_sand", GenesisBlocks.SUSPICIOUS_DARK_MOON_SAND);
    public static final DeferredItem<Item> DEAD_MOON_CORAL_BLOCK = blockItem("dead_moon_coral_block", GenesisBlocks.DEAD_MOON_CORAL_BLOCK);
    public static final DeferredItem<Item> DEAD_MOON_CORAL = blockItem("dead_moon_coral", GenesisBlocks.DEAD_MOON_CORAL);
    public static final DeferredItem<Item> DEAD_MOON_CORAL_FAN = ITEMS.register("dead_moon_coral_fan", () ->
            new StandingAndWallBlockItem(GenesisBlocks.DEAD_MOON_CORAL_FAN.get(), GenesisBlocks.DEAD_MOON_CORAL_WALL_FAN.get(),
                    new Item.Properties(), Direction.DOWN));

    // The moon's ice caverns

    public static final DeferredItem<Item> ICICLE = blockItem("icicle", GenesisBlocks.ICICLE);
    public static final DeferredItem<Item> ICE_CRYSTAL = blockItem("ice_crystal", GenesisBlocks.ICE_CRYSTAL);
    public static final DeferredItem<Item> THIN_ICE = blockItem("thin_ice", GenesisBlocks.THIN_ICE);
    public static final DeferredItem<Item> MOON_CRAWLER_SPAWN_EGG = ITEMS.register("moon_crawler_spawn_egg", () ->
            new DeferredSpawnEggItem(GenesisEntityTypes.MOON_CRAWLER, 0xCFE8F2, 0x3D6A8A, new Item.Properties()));

    private GenesisItems() {
    }

    private static DeferredItem<Item> blockItem(String name, DeferredBlock<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
