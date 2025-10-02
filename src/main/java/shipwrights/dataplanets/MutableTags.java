package shipwrights.dataplanets;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

public class MutableTags {
    public static final TagKey<Structure> WEATHER_LOW_BAND = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("dataplanets","weather_low_band"));
    public static final TagKey<Structure> QUEST_STRUCTURES = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("dataplanets","quest_structures"));
    public static final TagKey<Item> QUEST_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dataplanets","quest_items"));
    public static final TagKey<Block> SURFACE_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dataplanets","surface_blocks"));


    public static String[] getBlocksInTag(TagKey<Block> blockTag)
    {
        Optional<HolderSet.Named<Block>> optional = BuiltInRegistries.BLOCK.getTag(blockTag);
        if(optional.isEmpty())
        {
            return new String[] {"minecraft:cobblestone"};
        }
        List<Optional<ResourceKey<Block>>> keys = optional.get().stream().map(Holder::unwrapKey).toList();
        return keys.stream().filter(Optional::isPresent).map(Optional::get).map(a->a.location().toString()).toList().toArray(new String[0]);

    }
}
