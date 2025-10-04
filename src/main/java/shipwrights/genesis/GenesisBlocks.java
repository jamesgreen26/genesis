package shipwrights.genesis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.block.*;

public class GenesisBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GenesisMod.MOD_ID);

    public static final RegistryObject<Block> ASTEROID_0 = BLOCKS.register("asteroid_0", () ->
        new AsteroidBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> NAV_PROJECTOR = BLOCKS.register("nav_projector",
        () -> new NavProjectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> 15)));

    public static final RegistryObject<Block> VOID_ENGINE_INTERFACE = BLOCKS.register("void_engine_interface",
        () -> new VoidEngineInterfaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    public static final RegistryObject<Block> VOID_ENGINE_FRAME = BLOCKS.register("void_engine_frame",
        () -> new VoidEngineFrameBlock());

    public static final RegistryObject<Block> VOID_CORE = BLOCKS.register("void_core",
        () -> new VoidCoreBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> 15)));

    public static final RegistryObject<Block> VOID_ENGINE_VIEWPORT = BLOCKS.register("void_engine_viewport",
        () -> new VoidEngineViewportBlock());

    // Alien stones
    public static final RegistryObject<Block> VOIDSTONE = BLOCKS.register("voidstone", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> RIFTROCK = BLOCKS.register("riftrock", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> NULLSTONE = BLOCKS.register("nullstone", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> ECHOSTONE = BLOCKS.register("echostone", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> PHASEROCK = BLOCKS.register("phaserock", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> WARPSTONE = BLOCKS.register("warpstone", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .strength(1.5F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> WARPSTONE_ORE = BLOCKS.register("warpstone_ore", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .strength(3.0F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    // Alien sands
    public static final RegistryObject<Block> LUNAR_DUST = BLOCKS.register("lunar_dust", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .strength(0.5F)
            .sound(SoundType.SAND)
        )
    );

    public static final RegistryObject<Block> STELLAR_SAND = BLOCKS.register("stellar_sand", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .strength(0.5F)
            .sound(SoundType.SAND)
        )
    );
}