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
}
