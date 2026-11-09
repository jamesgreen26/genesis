package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales off-rail minecarts' fall, in and out of water, by {@link Gravity}
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartGravityMixin {
    @ModifyConstant(method = "tick", constant = {@Constant(doubleValue = -0.04), @Constant(doubleValue = -0.005)})
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
