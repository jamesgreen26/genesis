package shipwrights.genesis.content;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralPlantBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public final class GenesisBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GenesisMod.MOD_ID);

    // The moon

    public static final RegistryObject<Block> MOON_SAND = BLOCKS.register("moon_sand", () -> moonSand(MapColor.COLOR_LIGHT_GRAY));
    public static final RegistryObject<Block> WAVY_MOON_SAND = BLOCKS.register("wavy_moon_sand", () -> moonSand(MapColor.COLOR_LIGHT_GRAY));
    public static final RegistryObject<Block> DARK_MOON_SAND = BLOCKS.register("dark_moon_sand", () -> moonSand(MapColor.COLOR_GRAY));
    public static final RegistryObject<Block> DARK_WAVY_MOON_SAND = BLOCKS.register("dark_wavy_moon_sand", () -> moonSand(MapColor.COLOR_GRAY));
    // Brushable, as vanilla's suspicious sand, revealing its loot table's item and turning into dark moon sand
    public static final RegistryObject<Block> SUSPICIOUS_DARK_MOON_SAND = BLOCKS.register("suspicious_dark_moon_sand", () ->
            new BrushableBlock(DARK_MOON_SAND.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.25F)
                    .sound(SoundType.SUSPICIOUS_SAND)
                    .pushReaction(PushReaction.DESTROY),
                    SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND_COMPLETED));

    public static final RegistryObject<Block> MOON_STONE = BLOCKS.register("moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.5F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MOON_STONE_BRICKS = BLOCKS.register("moon_stone_bricks", () ->
            new Block(BlockBehaviour.Properties.copy(MOON_STONE.get())));
    public static final RegistryObject<Block> MOON_STONE_BRICK_SLAB = BLOCKS.register("moon_stone_brick_slab", () ->
            new SlabBlock(BlockBehaviour.Properties.copy(MOON_STONE_BRICKS.get())));
    public static final RegistryObject<Block> MOON_STONE_BRICK_STAIRS = BLOCKS.register("moon_stone_brick_stairs", () ->
            new StairBlock(() -> MOON_STONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(MOON_STONE_BRICKS.get())));
    public static final RegistryObject<Block> MOON_STONE_BRICK_WALL = BLOCKS.register("moon_stone_brick_wall", () ->
            new WallBlock(BlockBehaviour.Properties.copy(MOON_STONE_BRICKS.get()).forceSolidOn()));

    // Moon stone variants of the deepslate ores, with the same hardness, drops and behaviour

    public static final RegistryObject<Block> MOON_IRON_ORE = BLOCKS.register("moon_iron_ore", () ->
            new DropExperienceBlock(moonOre()));
    public static final RegistryObject<Block> MOON_REDSTONE_ORE = BLOCKS.register("moon_redstone_ore", () ->
            new RedStoneOreBlock(moonOre()
                    .randomTicks()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 9 : 0)));
    public static final RegistryObject<Block> MOON_LITHIUM_ORE = BLOCKS.register("moon_lithium_ore", () ->
            new DropExperienceBlock(moonOre()));
    /// Smelts into aluminum, as do the crystals it drops
    public static final RegistryObject<Block> ANORTHITE = BLOCKS.register("anorthite", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_ORE)
                    .mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.DEEPSLATE)));

    /// Basalt with energy crystals growing through it, found in the basalt formations of the moon's geothermal depths
    /// ({@link shipwrights.genesis.worldgen.BasaltFormationFeature})
    public static final RegistryObject<Block> CRYSTALLINE_BASALT = BLOCKS.register("crystalline_basalt", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.0F, 4.2F)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops(),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> HALLOW_MOON_STONE = BLOCKS.register("hallow_moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0F)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEAD_MOON_CORAL_BLOCK = BLOCKS.register("dead_moon_coral_block", () ->
            new Block(moonCoral()));
    public static final RegistryObject<Block> DEAD_MOON_CORAL_WALL_FAN = BLOCKS.register("dead_moon_coral_wall_fan", () ->
            new BaseCoralWallFanBlock(moonCoral().noCollission()));
    public static final RegistryObject<Block> DEAD_MOON_CORAL_FAN = BLOCKS.register("dead_moon_coral_fan", () ->
            new BaseCoralFanBlock(moonCoral().noCollission()));
    public static final RegistryObject<Block> DEAD_MOON_CORAL = BLOCKS.register("dead_moon_coral", () ->
            new BaseCoralPlantBlock(moonCoral().noCollission()));

    // The moon's ice caverns

    public static final RegistryObject<Block> ICICLE = BLOCKS.register("icicle", () ->
            new IcicleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false)));

    // A cluster pointing out from the surface it grows on, as vanilla's amethyst clusters do. Drops nothing (not even
    // itself with silk touch), so it can't be had in survival
    public static final RegistryObject<Block> ICE_CRYSTAL = BLOCKS.register("ice_crystal", () ->
            new AmethystClusterBlock(7, 3, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .forceSolidOn()
                    .noOcclusion()
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 5)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()));

    public static final RegistryObject<Block> THIN_ICE = BLOCKS.register("thin_ice", () ->
            new ThinIceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .friction(0.98F)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entity) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));

    private GenesisBlocks() {
    }

    private static Block moonSand(MapColor color) {
        return new FallingBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.5F)
                .sound(SoundType.SAND));
    }

    private static BlockBehaviour.Properties moonOre() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(4.5F, 3.0F)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties moonCoral() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.5F)
                .sound(SoundType.DECORATED_POT_CRACKED);
    }
}
