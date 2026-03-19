package shipwrights.genesis.content.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.*;
import shipwrights.genesis.GenesisMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.content.block.datagen.BlockType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
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
                output.accept(GenesisItems.TULCITE_CATALYZER_BLOCK_ITEM.get());
                output.accept(GenesisItems.TULCITE_CHUNK.get());
                output.accept(GenesisItems.VOID_SHARD.get());
                output.accept(GenesisItems.ANORTHITE_CRYSTAL.get());
                output.accept(GenesisItems.VOID_ENGINE_INTERFACE.get());
                output.accept(GenesisItems.VOID_CORE_REFLECTOR_PANEL.get());
                output.accept(GenesisItems.VOID_FOCUS.get());
                output.accept(GenesisItems.VOID_ENGINE_VIEWPORT.get());
                output.accept(GenesisItems.VOID_ENGINE_FRAME.get());
                output.accept(GenesisItems.VOID_CORE.get());
                output.accept(GenesisItems.SPACE_HELMET.get());
                output.accept(GenesisItems.SPACE_CHESTPLATE.get());
                output.accept(GenesisItems.SPACE_LEGGINGS.get());
                output.accept(GenesisItems.SPACE_BOOTS.get());
                addPaintings(parameters, output);
            })
            .build());

    private static final Set<String> alreadyAdded = new HashSet<>();

    public static final RegistryObject<CreativeModeTab> GENESIS_NATURAL_TAB = CREATIVE_MODE_TABS.register("genesis_natural_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.genesis_natural_tab"))
                    .icon(() -> new ItemStack(GenesisItems.NULLSTONE.get()))
                    .displayItems((parameters, output) -> {
                        addItemGroup(output, GenesisItems.WARPSTONE);
                        addItemGroup(output, GenesisItems.TULCITE_ORE);
                        addItemGroup(output, GenesisItems.VERDITE_ORE);
                        addItemGroup(output, GenesisItems.VOID_CORE_ORE);
                        addItemGroup(output, GenesisItems.ANORTHITE);
                        addItemGroup(output, GenesisItems.NULLSTONE);
                        addItemGroup(output, GenesisItems.VOIDSTONE);
                        addItemGroup(output, GenesisItems.RIFTROCK);
                        addItemGroup(output, GenesisItems.ECHOSTONE);
                        addItemGroup(output, GenesisItems.PHASEROCK);
                        output.accept(GenesisItems.STELLAR_SAND.get());
                        output.accept(GenesisItems.LUNAR_DUST.get());
                        output.accept(GenesisItems.CRACKED_CYAN_SALT.get());
                        output.accept(GenesisItems.CYAN_SALT.get());
                        output.accept(GenesisItems.CRACKED_TURQUOISE_SALT.get());
                        output.accept(GenesisItems.TURQUOISE_SALT.get());
                        output.accept(GenesisItems.CRACKED_RED_SALT.get());
                        output.accept(GenesisItems.RED_SALT.get());
                        output.accept(GenesisItems.CRACKED_PALE_RED_SALT.get());
                        output.accept(GenesisItems.PALE_RED_SALT.get());
                        output.accept(GenesisItems.SALT.get());
                        output.accept(GenesisItems.CRACKED_SALT.get());
                        output.accept(GenesisItems.BRINE_TRUNK.get());
                        output.accept(GenesisItems.BRINE_FLOWER.get());
                        output.accept(GenesisItems.PETRIFIED_BUSH.get());
                        output.accept(GenesisItems.HALLOW_MOON_STONE.get());
                        output.accept(GenesisItems.MOON_STONE.get());
                        output.accept(GenesisItems.DARK_WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_MOON_SAND.get());
                        output.accept(GenesisItems.MOON_SAND.get());
                        output.accept(GenesisItems.WAVY_MOON_SAND.get());
                        output.accept(GenesisItems.DARK_MOON_SAND.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_BLOCK.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL_FAN.get());
                        output.accept(GenesisItems.DEAD_MOON_CORAL.get());

                        output.accept(GenesisItems.CHALCOPYRITE.get());
                        output.accept(GenesisItems.CHALCOPYRITE_GRAVEL.get());
                        output.accept(GenesisItems.CHALCOPYRITE_SAND.get());
                        output.accept(GenesisItems.CHALCOPYRITE_SPROUTS.get());
                        output.accept(GenesisItems.CHALCOPYRITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_CHALCOPYRITE.get());
                        output.accept(GenesisItems.MALACHITE.get());
                        output.accept(GenesisItems.MALACHITE_GRAVEL.get());
                        output.accept(GenesisItems.MALACHITE_SAND.get());
                        output.accept(GenesisItems.MALACHITE_SPROUTS.get());
                        output.accept(GenesisItems.MALACHITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_MALACHITE.get());
                        output.accept(GenesisItems.HEMATITE.get());
                        output.accept(GenesisItems.HEMATITE_GRAVEL.get());
                        output.accept(GenesisItems.HEMATITE_SAND.get());
                        output.accept(GenesisItems.HEMATITE_SPROUTS.get());
                        output.accept(GenesisItems.HEMATITE_BRAMBLE.get());
                        output.accept(GenesisItems.ROOTED_SMOLDERING_HEMATITE.get());
                        output.accept(GenesisItems.SMOLDERING_LILY.get());

                        output.accept(GenesisItems.WITHERING_WILLOW_LOG.get());
                        output.accept(GenesisItems.WITHERING_WILLOW_BRANCH.get());
                        output.accept(GenesisItems.WITHERING_WILLOW_LEAVES.get());
                        output.accept(shipwrights.genesis.content.fluid.GenesisFluids.MIASMA.getBucket().get());

                        output.accept(GenesisItems.VERDITE_ORE.get());
                        output.accept(GenesisItems.VERDITE_CLUSTER.get());
                        output.accept(GenesisItems.VERDITE_CRYSTAL_BLOCK.get());

                        for (var item : GenesisItems.DYNAMIC_ITEMS.values()) {
                            if (alreadyAdded.contains(item.getId().getPath())) {
                                continue;
                            }
                            output.accept(item.get());
                        }
                    })
                    .build());

    private static void addItemGroup(CreativeModeTab.Output output, RegistryObject<Item> item) {
        output.accept(item.get());

        String name = item.getId().getPath();

        for (String suffix : BlockType.suffixes) {
            RegistryObject<Item> it = GenesisItems.DYNAMIC_ITEMS.get(name + suffix);
            if (it != null) {
                alreadyAdded.add(it.getId().getPath());
                output.accept(it.get());
            }
        }
    }


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
