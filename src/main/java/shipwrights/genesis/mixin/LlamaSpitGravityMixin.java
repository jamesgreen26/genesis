package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales llama spit's fall by {@link Gravity}
@Mixin(LlamaSpit.class)
public abstract class LlamaSpitGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = (double) -0.06F))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
