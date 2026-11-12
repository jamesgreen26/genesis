package shipwrights.genesis.content;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.crawler.MoonCrawler;

public final class GenesisEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, GenesisMod.MOD_ID);

    /// A cave spider variant living in the moon's ice caverns
    public static final DeferredHolder<EntityType<?>, EntityType<MoonCrawler>> MOON_CRAWLER = ENTITY_TYPES.register("moon_crawler", () ->
            EntityType.Builder.of(MoonCrawler::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.5F)
                    .clientTrackingRange(8)
                    .build(GenesisMod.id("moon_crawler").toString()));

    private GenesisEntityTypes() {
    }
}
