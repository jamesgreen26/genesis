package shipwrights.genesis.content.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.block.datagen.BlockDataGenerator;
import shipwrights.genesis.content.item.GenesisItems;
import shipwrights.genesis.mixin.BlockBehaviourAccessor;

import java.util.Optional;

public class GenesisBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GenesisMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, GenesisMod.MOD_ID);

    public static final RegistryObject<Block> ASTEROID_0 = BLOCKS.register("asteroid_0", () ->
        new AsteroidBlock(BlockBehaviour.Properties.copy(Blocks.BARRIER)
                .mapColor(DyeColor.GRAY).sound(SoundType.STONE)
        )
    );

    static Optional<RegistryObject<Block>> blockLookup(String name) {
        for (var block: BLOCKS.getEntries()) {
            ResourceLocation id = block.getId();
            if (id != null && id.getPath().equals(name)) {
                return Optional.of(block);
            }
        }
        return Optional.empty();
    }

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

    public static final RegistryObject<Block> TULCITE_ORE = BLOCKS.register("tulcite_ore", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<Block> VERDITE_ORE = BLOCKS.register("verdite_ore", () ->
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .strength(3.0F, 6.0F)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<VoidCoreOreBlock> VOID_CORE_ORE = BLOCKS.register("void_core_ore",
            () -> new VoidCoreOreBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(6.0F, 12.0F)
                    .sound(SoundType.AMETHYST)
            )
    );

    public static final RegistryObject<DropExperienceBlock> ANORTHITE = BLOCKS.register("anorthite",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_ORE)
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.DEEPSLATE)
            )
    );

    // Alien sands
    public static final RegistryObject<Block> LUNAR_DUST = BLOCKS.register("lunar_dust", () ->
        new FallingBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .strength(0.5F)
            .sound(SoundType.SAND)
        )
    );

    public static final RegistryObject<Block> STELLAR_SAND = BLOCKS.register("stellar_sand", () ->
        new FallingBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .strength(0.5F)
            .sound(SoundType.SAND)
        )
    );

    public static final RegistryObject<Block> RED_SALT = BLOCKS.register("red_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> PALE_RED_SALT = BLOCKS.register("pale_red_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_RED_SALT = BLOCKS.register("cracked_red_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_PALE_RED_SALT = BLOCKS.register("cracked_pale_red_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CYAN_SALT = BLOCKS.register("cyan_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> TURQUOISE_SALT = BLOCKS.register("turquoise_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_NYLIUM)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_CYAN_SALT = BLOCKS.register("cracked_cyan_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_TURQUOISE_SALT = BLOCKS.register("cracked_turquoise_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_NYLIUM)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> SALT = BLOCKS.register("salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CRACKED_SALT = BLOCKS.register("cracked_salt", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> MOON_SAND = BLOCKS.register("moon_sand", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> WAVY_MOON_SAND = BLOCKS.register("wavy_moon_sand", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> DARK_MOON_SAND = BLOCKS.register("dark_moon_sand", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> DARK_WAVY_MOON_SAND = BLOCKS.register("dark_wavy_moon_sand", () ->
            new FallingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> MOON_STONE = BLOCKS.register("moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.5F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<Block> HALLOW_MOON_STONE = BLOCKS.register("hallow_moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0F)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_BLOCK = BLOCKS.register("dead_moon_coral_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.DECORATED_POT_CRACKED)
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_WALL_FAN = BLOCKS.register("dead_moon_coral_wall_fan", () ->
            new BaseCoralWallFanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.DECORATED_POT_CRACKED)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL_FAN = BLOCKS.register("dead_moon_coral_fan", () ->
            new BaseCoralFanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.DECORATED_POT_CRACKED)
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> DEAD_MOON_CORAL = BLOCKS.register("dead_moon_coral", () ->
            new BaseCoralPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.DECORATED_POT_CRACKED)
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

    public static final RegistryObject<Block> BUBBLE_SHROOM_STALK = BLOCKS.register("bubble_shroom_stalk", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.NETHER_WOOD)
            )
    );

    public static final RegistryObject<Block> BUBBLE_SHROOM_CAP = BLOCKS.register("bubble_shroom_cap", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5F)
                    .sound(SoundType.NETHER_WOOD)
            )
    );

    public static final RegistryObject<Block> BEARD_SHROOM_CAP = BLOCKS.register("beard_shroom_cap", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5F)
                    .sound(SoundType.NETHER_WOOD)
            )
    );

    public static final RegistryObject<Block> BEARD_SHROOM_LOG = BLOCKS.register("beard_shroom_log", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F)
                    .sound(SoundType.NETHER_WOOD)
            )
    );

    public static final RegistryObject<Block> BEARD_SHROOM_LEAVES = BLOCKS.register("beard_shroom_leaves", () ->
            new LeavesBlock(BlockBehaviour.Properties
                    .copy(Blocks.CHERRY_LEAVES)
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .sound(SoundType.CHERRY_LEAVES)
            )
    );

    public static final RegistryObject<Block> FLOWERING_BEARD_SHROOM_LEAVES = BLOCKS.register("flowering_beard_shroom_leaves", () ->
            new CherryLeavesBlock(BlockBehaviour.Properties
                    .copy(Blocks.CHERRY_LEAVES)
                    .mapColor(MapColor.COLOR_PINK)
                    .sound(SoundType.CHERRY_LEAVES)
            )
    );

    public static final RegistryObject<Block> HANGING_BEARD_SHROOM_LEAVES = BLOCKS.register("hanging_beard_shroom_leaves", () ->
            new WeepingVinesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.0F)
                    .sound(SoundType.CHERRY_LEAVES)
                    .noCollission()
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> HANGING_BEARD_SHROOM_LEAVES_PLANT = BLOCKS.register("hanging_beard_shroom_leaves_plant", () ->
            new WeepingVinesPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.0F)
                    .sound(SoundType.CHERRY_LEAVES)
                    .noCollission()
                    .noOcclusion()
            )
    );



    public static final RegistryObject<Block> ZAPLIGHT = BLOCKS.register("zaplight", () ->
            new Block(BlockBehaviour.Properties
                    .copy(Blocks.SHROOMLIGHT)
                    .mapColor(MapColor.COLOR_PINK)
                    .sound(SoundType.SHROOMLIGHT)
                    .emissiveRendering((state, level, pos) -> true)
                    .lightLevel(state -> 15)
            )
    );

    public static final RegistryObject<Block> ZAPLIGHT_SPINDLES = BLOCKS.register("zaplight_spindles", () ->
            new ZaplightSpindlesBlock(BlockBehaviour.Properties
                    .copy(Blocks.DEAD_BRAIN_CORAL)
                    .mapColor(MapColor.COLOR_PINK)
                    .sound(SoundType.CHERRY_LEAVES)
                    .emissiveRendering((state, level, pos) -> true)
                    .lightLevel(state -> 15)
            )
    );

    public static final RegistryObject<Block> WALL_ZAPLIGHT_SPINDLES = BLOCKS.register("wall_zaplight_spindles", () ->
            new WallZaplightSpindlesBlock(BlockBehaviour.Properties
                    .copy(Blocks.DEAD_BRAIN_CORAL_WALL_FAN)
                    .mapColor(MapColor.COLOR_PINK)
                    .sound(SoundType.CHERRY_LEAVES)
                    .emissiveRendering((state, level, pos) -> true)
                    .lightLevel(state -> 15)
            )
    );

    public static final RegistryObject<Block> AZURE_MOSS = BLOCKS.register("azure_moss", () ->
            new Block(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_BLOCK)
                    .mapColor(MapColor.COLOR_BLUE)
                    .sound(SoundType.MOSS)
            )
    );

    public static final RegistryObject<Block> CYAN_MOSS = BLOCKS.register("cyan_moss", () ->
            new Block(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_BLOCK)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> TURQUOISE_MOSS = BLOCKS.register("turquoise_moss", () ->
            new Block(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_BLOCK)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> AZURE_MOSS_CARPET = BLOCKS.register("azure_moss_carpet", () ->
            new CarpetBlock(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_CARPET)
                    .mapColor(MapColor.COLOR_BLUE)
                    .sound(SoundType.MOSS)
            )
    );

    public static final RegistryObject<Block> CYAN_MOSS_CARPET = BLOCKS.register("cyan_moss_carpet", () ->
            new CarpetBlock(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_CARPET)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> TURQUOISE_MOSS_CARPET = BLOCKS.register("turquoise_moss_carpet", () ->
            new CarpetBlock(BlockBehaviour.Properties
                    .copy(Blocks.MOSS_CARPET)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );



    public static final RegistryObject<Block> SPINDLE_GRASS = BLOCKS.register("spindle_grass", () ->
            new AzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> SPINDLE_BUSH = BLOCKS.register("spindle_bush", () ->
            new AzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> GLOWING_SPINDLE_BUSH = BLOCKS.register("glowing_spindle_bush", () ->
            new AzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> TALL_SPINDLE_GRASS = BLOCKS.register("tall_spindle_grass", () ->
            new TallAzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> GIANT_SPINDLE_GRASS = BLOCKS.register("gaint_spindle_grass", () ->
            new TallAzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> BELL_FLOWER = BLOCKS.register("bell_flower", () ->
            new TallAzurePlantBlock(BlockBehaviour.Properties
                    .copy(Blocks.GRASS)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> HOOK_GRASS = BLOCKS.register("hook_grass", () ->
            new AzurePlantBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XYZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .mapColor(MapColor.COLOR_BLUE)
            )
    );

    public static final RegistryObject<Block> VERDITE_CRYSTAL_BLOCK = BLOCKS.register("verdite_crystal_block", () ->
            new VerditeCrystalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.AMETHYST)
                    .randomTicks()
                    .lightLevel(state -> 15)
            )
    );

    public static final RegistryObject<Block> SMALL_VERDITE_BUD = BLOCKS.register("small_verdite_bud", () ->
            new AmethystClusterBlock(2,2,BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
            )
    );

    public static final RegistryObject<Block> MEDIUM_VERDITE_BUD = BLOCKS.register("medium_verdite_bud", () ->
            new AmethystClusterBlock(2,2,BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 6)
            )
    );

    public static final RegistryObject<Block> LARGE_VERDITE_BUD = BLOCKS.register("large_verdite_bud", () ->
            new AmethystClusterBlock(2,2,BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
            )
    );

    public static final RegistryObject<Block> VERDITE_CLUSTER = BLOCKS.register("verdite_cluster", () ->
            new AmethystClusterBlock(2,2,BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)
            )
    );

    public static final RegistryObject<Block> CHALCOPYRITE_SAND = BLOCKS.register("chalcopyrite_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> MALACHITE_SAND = BLOCKS.register("malachite_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> HEMATITE_SAND = BLOCKS.register("hematite_sand", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.SAND)
            )
    );

    public static final RegistryObject<Block> CHALCOPYRITE_GRAVEL = BLOCKS.register("chalcopyrite_gravel", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> MALACHITE_GRAVEL = BLOCKS.register("malachite_gravel", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> HEMATITE_GRAVEL = BLOCKS.register("hematite_gravel", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> CHALCOPYRITE = BLOCKS.register("chalcopyrite", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.NETHER_BRICKS)
            )
    );

    public static final RegistryObject<Block> MALACHITE = BLOCKS.register("malachite", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.NETHER_BRICKS)
            )
    );

    public static final RegistryObject<Block> HEMATITE = BLOCKS.register("hematite", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.NETHER_BRICKS)
            )
    );

    public static final RegistryObject<Block> ROOTED_SMOLDERING_CHALCOPYRITE = BLOCKS.register("rooted_smoldering_chalcopyrite", () ->
            new SmolderingRootBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> ROOTED_SMOLDERING_MALACHITE = BLOCKS.register("rooted_smoldering_malachite", () ->
            new SmolderingRootBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> ROOTED_SMOLDERING_HEMATITE = BLOCKS.register("rooted_smoldering_hematite", () ->
            new SmolderingRootBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final RegistryObject<Block> CHALCOPYRITE_SPROUTS = BLOCKS.register("chalcopyrite_sprouts", () ->
            new BaseCoralPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.NETHER_SPROUTS)
                    .noCollission()
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> MALACHITE_SPROUTS = BLOCKS.register("malachite_sprouts", () ->
            new BaseCoralPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.NETHER_SPROUTS)
                    .noCollission()
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> HEMATITE_SPROUTS = BLOCKS.register("hematite_sprouts", () ->
            new BaseCoralPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.NETHER_SPROUTS)
                    .noCollission()
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> CHALCOPYRITE_BRAMBLE = BLOCKS.register("chalcopyrite_bramble", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.CHERRY_LEAVES)
                    .strength(0.5F)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> MALACHITE_BRAMBLE = BLOCKS.register("malachite_bramble", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .sound(SoundType.CHERRY_LEAVES)
                    .strength(0.5F)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> HEMATITE_BRAMBLE = BLOCKS.register("hematite_bramble", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .sound(SoundType.CHERRY_LEAVES)
                    .strength(0.5F)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> RAINBOW_CHALCOPYRITE_BLOCK = BLOCKS.register("rainbow_chalcopyrite_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .sound(SoundType.CALCITE)
                    .strength(0.5F)
            )
    );

    public static final RegistryObject<Block> RAINBOW_CHALCOPYRITE_CLUSTER = BLOCKS.register("rainbow_chalcopyrite_cluster", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .sound(SoundType.CALCITE)
                    .strength(0.5F)
            )
    );

    public static final RegistryObject<Block> RAINBOW_CHALCOPYRITE_BRICK = BLOCKS.register("rainbow_chalcopyrite_brick", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .sound(SoundType.CALCITE)
                    .strength(0.5F)
            )
    );

    public static final RegistryObject<Block> WITHERING_WILLOW_LOG = BLOCKS.register("withering_willow_log", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.WOOD)
            )
    );

    public static final RegistryObject<Block> WITHERING_WILLOW_BRANCH = BLOCKS.register("withering_willow_branch", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission()
            )
    );

    public static final RegistryObject<Block> WITHERING_WILLOW_LEAVES = BLOCKS.register("withering_willow_leaves", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.AZALEA_LEAVES)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Block> WITHERING_WILLOW_PLANKS = BLOCKS.register("withering_willow_planks", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.WOOD)
            )
    );

    public static final RegistryObject<Block> SMOLDERING_LILY = BLOCKS.register("smoldering_lily", () ->
            new SmolderingLilyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PURPLE)
                    .sound(SoundType.AZALEA_LEAVES)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(state -> 16)
            )
    );

    public static final RegistryObject<Block> SMOLDERING_TURNIP = BLOCKS.register("smoldering_turnip", () ->
            new BaseCoralPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .sound(SoundType.NETHER_WOOD)
            )
    );

    public static final RegistryObject<Block> VOID_CORE_REFLECTOR_PANEL = BLOCKS.register("void_core_reflector_panel", () ->
            new VoidCorePanelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
            )
    );


    public static final RegistryObject<Block> VERDITE_VOID_COIL = BLOCKS.register("verdite_void_coil", () ->
            new VerditeVoidCoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
            )
    );

    public static final RegistryObject<Block> VOID_FOCUS = BLOCKS.register("void_focus", () ->
            new VoidFocusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
            )
    );



    public static final RegistryObject<TulciteCatalyzerBlock> TULCITE_CATALYZER_BLOCK = BLOCKS.register("tulcite_catalyzer_block", TulciteCatalyzerBlock::new);

    public static final RegistryObject<MenuType<TulciteCatalyzerContainer>> TULCITE_CATALYZER_CONTAINER = MENU_TYPES.register("tulcite_catalyzer_block",
            () -> IForgeMenuType.create((windowId, inv, data) -> new TulciteCatalyzerContainer(windowId, inv.player, data.readBlockPos())));


    static {
        for (var entry : BlockDataGenerator.blocksToDatagen.entrySet()) {
            for (var type : entry.getValue()) {
                switch (type) {
                    case simple -> { /* registered manually */}
                    case slab -> {
                        String id = entry.getKey() + "_slab";
                        RegistryObject<Block> slab = BLOCKS.register(id,
                                () -> new SlabBlock(((BlockBehaviourAccessor) blockLookup(entry.getKey()).get().get()).getProperties()));

                        GenesisItems.DYNAMIC_ITEMS.put(id, GenesisItems.ITEMS.register(id,
                                () -> new BlockItem(slab.get(), new Item.Properties())));
                    }
                    case stairs -> {
                        String id = entry.getKey() + "_stairs";
                        RegistryObject<Block> stairs = BLOCKS.register(id,
                                () -> {
                                    Block block = blockLookup(entry.getKey()).get().get();
                                    return new StairBlock(block::defaultBlockState, ((BlockBehaviourAccessor) block).getProperties());
                                });

                        GenesisItems.DYNAMIC_ITEMS.put(id, GenesisItems.ITEMS.register(id,
                                () -> new BlockItem(stairs.get(), new Item.Properties())));
                    }
                }
            }
        }
    }
}