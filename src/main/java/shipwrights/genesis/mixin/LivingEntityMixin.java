package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.content.crawler.MoonCrawler;

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
}
