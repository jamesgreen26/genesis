package shipwrights.genesis.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import shipwrights.genesis.GenesisMod;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> SALT_PLANTABLE = tag("salt_plantable");
        public static final TagKey<Block> BRINE_TRUNK_PLANTABLE = tag("brine_trunk_plantable");

        private static TagKey<Block> tag(String name){
            return BlockTags.create(new ResourceLocation(GenesisMod.MOD_ID, name));
        }
    }

    public static class Items {


        private static TagKey<Item> tag(String name){
            return ItemTags.create(new ResourceLocation(GenesisMod.MOD_ID, name));
        }
    }
}
