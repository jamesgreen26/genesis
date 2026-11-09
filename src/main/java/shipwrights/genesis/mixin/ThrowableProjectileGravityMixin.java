package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.space.Gravity;

/// Scales thrown projectiles' (snowballs, eggs, potions...) fall by {@link Gravity}. Hooks where {@code getGravity} is
/// used rather than the method itself, so subclasses overriding it are scaled too
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGravityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;getGravity()F"))
    private float genesis$scaleGravity(float gravity) {
        return (float) (gravity * Gravity.multiplier(((Entity) (Object) this).level()));
    }
}
