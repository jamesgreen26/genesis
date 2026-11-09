package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import shipwrights.genesis.space.Gravity;

/// Scales dropped items' fall by {@link Gravity}
@Mixin(ItemEntity.class)
public abstract class ItemEntityGravityMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = -0.04))
    private double genesis$scaleGravity(double gravity) {
        return gravity * Gravity.multiplier(((Entity) (Object) this).level());
    }
}
