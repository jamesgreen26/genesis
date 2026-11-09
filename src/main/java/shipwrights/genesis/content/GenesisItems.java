package shipwrights.genesis.content;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public final class GenesisItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GenesisMod.MOD_ID);

    // The moon

    public static final RegistryObject<Item> MOON_STONE = blockItem("moon_stone", GenesisBlocks.MOON_STONE);
    public static final RegistryObject<Item> HALLOW_MOON_STONE = blockItem("hallow_moon_stone", GenesisBlocks.HALLOW_MOON_STONE);
    public static final RegistryObject<Item> MOON_SAND = blockItem("moon_sand", GenesisBlocks.MOON_SAND);
    public static final RegistryObject<Item> WAVY_MOON_SAND = blockItem("wavy_moon_sand", GenesisBlocks.WAVY_MOON_SAND);
    public static final RegistryObject<Item> DARK_MOON_SAND = blockItem("dark_moon_sand", GenesisBlocks.DARK_MOON_SAND);
    public static final RegistryObject<Item> DARK_WAVY_MOON_SAND = blockItem("dark_wavy_moon_sand", GenesisBlocks.DARK_WAVY_MOON_SAND);
    public static final RegistryObject<Item> DEAD_MOON_CORAL_BLOCK = blockItem("dead_moon_coral_block", GenesisBlocks.DEAD_MOON_CORAL_BLOCK);
    public static final RegistryObject<Item> DEAD_MOON_CORAL = blockItem("dead_moon_coral", GenesisBlocks.DEAD_MOON_CORAL);
    public static final RegistryObject<Item> DEAD_MOON_CORAL_FAN = ITEMS.register("dead_moon_coral_fan", () ->
            new StandingAndWallBlockItem(GenesisBlocks.DEAD_MOON_CORAL_FAN.get(), GenesisBlocks.DEAD_MOON_CORAL_WALL_FAN.get(),
                    new Item.Properties(), Direction.DOWN));

    // The moon's ice caverns

    public static final RegistryObject<Item> ICICLE = blockItem("icicle", GenesisBlocks.ICICLE);
    public static final RegistryObject<Item> ICE_CRYSTAL = blockItem("ice_crystal", GenesisBlocks.ICE_CRYSTAL);
    public static final RegistryObject<Item> BRITTLE_ICE = blockItem("brittle_ice", GenesisBlocks.BRITTLE_ICE);
    public static final RegistryObject<Item> MOON_CRAWLER_SPAWN_EGG = ITEMS.register("moon_crawler_spawn_egg", () ->
            new ForgeSpawnEggItem(GenesisEntityTypes.MOON_CRAWLER, 0xCFE8F2, 0x3D6A8A, new Item.Properties()));

    private GenesisItems() {
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
