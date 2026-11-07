package shipwrights.genesis.content;

import net.minecraft.world.level.block.BaseCoralFanBlock;
import net.minecraft.world.level.block.BaseCoralPlantBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final RegistryObject<Block> MOON_STONE = BLOCKS.register("moon_stone", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.5F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

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

    private GenesisBlocks() {
    }

    private static Block moonSand(MapColor color) {
        return new FallingBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.5F)
                .sound(SoundType.SAND));
    }

    private static BlockBehaviour.Properties moonCoral() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.5F)
                .sound(SoundType.DECORATED_POT_CRACKED);
    }
}
