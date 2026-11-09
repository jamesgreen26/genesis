package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales arrows' and tridents' drop by {@link Gravity}
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = (double) 0.05F))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
