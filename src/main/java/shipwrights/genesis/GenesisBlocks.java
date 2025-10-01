package shipwrights.genesis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.block.AsteroidBlock;

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
}