package shipwrights.dataplanets;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;

public class MutableTags {
    public static final TagKey<Structure> WEATHER_LOW_BAND = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("dataplanets","weather_low_band"));
    public static final TagKey<Structure> QUEST_STRUCTURES = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("dataplanets","quest_structures"));
    public static final TagKey<Item> QUEST_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("dataplanets","quest_items"));

}
