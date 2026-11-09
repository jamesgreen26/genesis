package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales experience orbs' fall by {@link Gravity}
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = -0.03))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
