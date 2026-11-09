package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales primed TNT's fall by {@link Gravity}
@Mixin(PrimedTnt.class)
public abstract class PrimedTntGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = -0.04))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
