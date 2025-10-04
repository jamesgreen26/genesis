package shipwrights.genesis.item;

import shipwrights.genesis.GenesisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class GenesisCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GenesisMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GENESIS_TAB = CREATIVE_MODE_TABS.register("genesis_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.genesis_tab"))
            .icon(() -> new ItemStack(GenesisItems.NAV_PROJECTOR.get()))
            .displayItems((parameters, output) -> {
                output.accept(GenesisItems.NAV_PROJECTOR.get());
                output.accept(GenesisItems.VOID_ENGINE_INTERFACE.get());
                output.accept(GenesisItems.VOID_ENGINE_FRAME.get());
                output.accept(GenesisItems.VOID_ENGINE_VIEWPORT.get());
                output.accept(GenesisItems.VOID_CORE.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
