package shipwrights.genesis.content.crawler;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import shipwrights.genesis.content.GenesisMobEffects;

/// A cave spider adapted to the moon's ice caverns. Its bite poisons and webs its target, it grips ice without
/// slipping (see {@link shipwrights.genesis.mixin.LivingEntityMixin}) and it's unaffected by the vacuum and cold
/// (see {@link MoonCrawlerEvents})
public class MoonCrawler extends CaveSpider {
    public MoonCrawler(EntityType<? extends MoonCrawler> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CaveSpider.createCaveSpider();
    }

    /// Only 1 in this many natural spawn attempts succeed, keeping the ice caverns from swarming with crawlers
    private static final int NATURAL_SPAWN_CHANCE = 4;

    public static boolean checkMoonCrawlerSpawnRules(EntityType<MoonCrawler> type, ServerLevelAccessor level,
                                                     MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL && random.nextInt(NATURAL_SPAWN_CHANCE) != 0) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // CaveSpider applies poison
        if (!super.doHurtTarget(target)) {
            return false;
        }

        if (target instanceof LivingEntity living) {
            Difficulty difficulty = level().getDifficulty();
            int seconds = difficulty == Difficulty.NORMAL ? 7 : difficulty == Difficulty.HARD ? 15 : 0;
            if (seconds > 0) {
                living.addEffect(new MobEffectInstance(GenesisMobEffects.WEBBED, seconds * 20, 0), this);
            }
        }
        return true;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }
}
