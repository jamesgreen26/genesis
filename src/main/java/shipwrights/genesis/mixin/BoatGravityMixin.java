package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales boats' fall by {@link Gravity}. Vanilla also assigns this constant to an unused local, which scaling leaves harmless
@Mixin(Boat.class)
public abstract class BoatGravityMixin {
    @ModifyConstant(method = "floatBoat", constant = @Constant(doubleValue = (double) -0.04F))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
