package shipwrights.genesis;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.block.*;

public class GenesisBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GenesisMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, GenesisMod.MOD_ID);

    public static final RegistryObject<Block> ASTEROID_0 = BLOCKS.register("asteroid_0", () ->
        new AsteroidBlock(BlockBehaviour.Properties.copy(Blocks.BARRIER)
                .mapColor(DyeColor.GRAY).sound(SoundType.STONE)
        )
    );

    public static final RegistryObject<Block> NAV_PROJECTOR = BLOCKS.register("nav_projector",
        () -> new NavProjectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> 15)));

    public static final RegistryObject<Block> RADAR_DISPLAY = BLOCKS.register("radar_display",
        () -> new RadarDisplayBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

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

    public static final RegistryObject<Block> RED_SALT = BLOCKS.register("red_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> PALE_RED_SALT = BLOCKS.register("pale_red_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_RED_SALT = BLOCKS.register("cracked_red_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_PALE_RED_SALT = BLOCKS.register("cracked_pale_red_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CYAN_SALT = BLOCKS.register("cyan_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> TURQUOISE_SALT = BLOCKS.register("turquoise_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_NYLIUM)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_CYAN_SALT = BLOCKS.register("cracked_cyan_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_TURQUOISE_SALT = BLOCKS.register("cracked_turquoise_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_NYLIUM)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> SALT = BLOCKS.register("salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_SALT = BLOCKS.register("cracked_salt", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> MOON_SAND = BLOCKS.register("moon_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> WAVY_MOON_SAND = BLOCKS.register("wavy_moon_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> DARK_MOON_SAND = BLOCKS.register("dark_moon_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> DARK_WAVY_MOON_SAND = BLOCKS.register("dark_wavy_moon_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> MOON_STONE = BLOCKS.register("moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.STONE)
            )
    );

    public static final RegistryObject<Block> HALLOW_MOON_STONE = BLOCKS.register("hallow_moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.STONE)
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_BLOCK = BLOCKS.register("dead_moon_coral_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.STONE)
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_WALL_FAN = BLOCKS.register("dead_moon_coral_wall_fan", () ->
            new BaseCoralWallFanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_FAN = BLOCKS.register("dead_moon_coral_fan", () ->
            new BaseCoralFanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL = BLOCKS.register("dead_moon_coral", () ->
            new BaseCoralPlantTypeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> BRINE_TRUNK = BLOCKS.register("brine_trunk", () ->
            new BrineTrunkPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F)
                    .sound(SoundType.TUFF)
            )
    );

    public static final RegistryObject<Block> BRINE_FLOWER = BLOCKS.register("brine_flower", () ->
            new BrineFlowerBlock((BrineTrunkPlantBlock) BRINE_TRUNK.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.TUFF)
                    .emissiveRendering((state, level, pos) -> true)
                    .lightLevel(state -> 12)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> PETRIFIED_BUSH = BLOCKS.register("petrified_bush", () ->
            new ColoredSaltPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F)
                    .sound(SoundType.TUFF)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> MIMIC_FEATHER = BLOCKS.register("mimic_feather", () ->
            new BushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> TALL_MIMIC_FEATHER = BLOCKS.register("tall_mimic_feather", () ->
            new TallFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
                    .noCollission()
            )
    );


    public static final RegistryObject<WarpstoneCatalyzerBlock> WARPSTONE_CATALYZER_BLOCK = BLOCKS.register("warpstone_catalyzer_block", WarpstoneCatalyzerBlock::new);

    public static final RegistryObject<MenuType<WarpstoneCatalyzerContainer>> WARPSTONE_CATALYZER_CONTAINER = MENU_TYPES.register("warpstone_catalyzer_block",
            () -> IForgeMenuType.create((windowId, inv, data) -> new WarpstoneCatalyzerContainer(windowId, inv.player, data.readBlockPos())));
}