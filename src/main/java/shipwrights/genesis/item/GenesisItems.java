package shipwrights.genesis.item;

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
}
