package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales falling blocks' (sand, gravel...) fall by {@link Gravity}
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = -0.04))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
