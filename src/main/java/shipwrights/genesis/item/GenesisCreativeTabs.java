package shipwrights.genesis.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import shipwrights.genesis.GenesisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
import java.util.function.Predicate;

public class GenesisCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GenesisMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GENESIS_TAB = CREATIVE_MODE_TABS.register("genesis_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.genesis_tab"))
            .icon(() -> new ItemStack(GenesisItems.NAV_PROJECTOR.get()))
            .displayItems((parameters, output) -> {
                output.accept(GenesisItems.NAV_PROJECTOR.get());
                output.accept(GenesisItems.RADAR_DISPLAY.get());
                output.accept(GenesisItems.WARPSTONE_CATALYZER_BLOCK_ITEM.get());
                output.accept(GenesisItems.WARPSTONE_CHUNK.get());
                output.accept(GenesisItems.VOID_ENGINE_INTERFACE.get());
                output.accept(GenesisItems.VOID_ENGINE_FRAME.get());
                output.accept(GenesisItems.VOID_ENGINE_VIEWPORT.get());
                output.accept(GenesisItems.VOID_CORE.get());
                addPaintings(parameters, output);
            })
            .build());

    private static void addPaintings(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        parameters.holders().lookup(Registries.PAINTING_VARIANT).ifPresent((arg2x) -> generatePresetPaintings(output, arg2x, (arg) -> arg.is(PaintingVariantTags.PLACEABLE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }

    private static void generatePresetPaintings(CreativeModeTab.Output arg, HolderLookup.RegistryLookup<PaintingVariant> arg2, Predicate<Holder<PaintingVariant>> predicate, CreativeModeTab.TabVisibility arg3) {
        arg2.listElements().filter(predicate).sorted(PAINTING_COMPARATOR).forEach((arg3x) -> {
            if (arg3x.key().location().getNamespace().equals("genesis")) {
                ItemStack itemstack = new ItemStack(Items.PAINTING);
                CompoundTag compoundtag = itemstack.getOrCreateTagElement("EntityTag");
                Painting.storeVariant(compoundtag, arg3x);
                arg.accept(itemstack, arg3);
            }
        });
    }

    private static final Comparator<Holder<PaintingVariant>> PAINTING_COMPARATOR = Comparator.comparing(Holder::value, Comparator.comparingInt((PaintingVariant arg) -> arg.getHeight() * arg.getWidth()).thenComparing(PaintingVariant::getWidth));


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
