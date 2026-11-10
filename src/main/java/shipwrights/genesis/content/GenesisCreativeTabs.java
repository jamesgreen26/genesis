package shipwrights.genesis.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public final class GenesisCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GenesisMod.MOD_ID);

    /// Everything Genesis adds
    public static final RegistryObject<CreativeModeTab> GENESIS_TAB = CREATIVE_MODE_TABS.register("genesis", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.genesis"))
                    .icon(() -> new ItemStack(GenesisItems.MOON_STONE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(GenesisItems.HALLOW_MOON_STONE.get());
                        output.accept(GenesisItems.MOON_STONE.get());
                        output.accept(GenesisItems.MOON_STONE_BRICKS.get());
                        output.accept(GenesisItems.MOON_STONE_BRICK_STAIRS.get());
                        output.accept(GenesisItems.MOON_STONE_BRICK_SLAB.get());
                        output.accept(GenesisItems.MOON_STONE_BRICK_WALL.get());
                        output.accept(GenesisItems.MOON_IRON_ORE.get());
                        output.accept(GenesisItems.MOON_REDSTONE_ORE.get());
                        output.accept(GenesisItems.MOON_LITHIUM_ORE.get());
                        output.accept(GenesisItems.ANORTHITE.get());
                        output.accept(GenesisItems.CRYSTALLINE_BASALT.get());
                        output.accept(GenesisItems.MOON_SAND.get());
                        output.accept(GenesisItems.WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.SUSPICIOUS_DARK_MOON_SAND.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_BLOCK.get());
                        output.accept(GenesisItems.THIN_ICE.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_FAN.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL.get());
                        output.accept(GenesisItems.ICICLE.get());
                        output.accept(GenesisItems.ICE_CRYSTAL.get());
                        output.accept(GenesisItems.ANORTHITE_CRYSTAL.get());
                        output.accept(GenesisItems.ZIRCON.get());
                        output.accept(GenesisItems.MOON_CRAWLER_SPAWN_EGG.get());
                    })
                    .build());

    private GenesisCreativeTabs() {
    }
}
