package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.content.crawler.MoonCrawler;
import shipwrights.genesis.space.Gravity;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /// Default block friction; anything above this (ice) is slippery
    private static final float GENESIS$NON_SLIP_FRICTION = 0.6F;

    /// Moon crawlers grip ice instead of sliding on it
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"))
    private float genesis$moonCrawlerGripsIce(float friction) {
        if ((Object) this instanceof MoonCrawler) {
            return Math.min(friction, GENESIS$NON_SLIP_FRICTION);
        }
        return friction;
    }

    /// Scales players' and mobs' gravity by {@link Gravity}. The first attribute read in {@code travel} is Forge's
    /// entity gravity, which already includes Slow Falling, so that scales too
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D", ordinal = 0))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((LivingEntity) (Object) this).level());
    }
}
