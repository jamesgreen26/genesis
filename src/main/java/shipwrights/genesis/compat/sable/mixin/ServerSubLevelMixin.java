package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.GenesisMod;

/// Space has no void, nor any height too great. Sable removes sub-levels below and above heights set in its config, as
/// vanilla removes entities in the void, in every dimension; but not in space-like ones, where nothing else is (see
/// {@link shipwrights.genesis.mixin.EntityMixin})
@Mixin(targets = "dev.ryanhcode.sable.sublevel.ServerSubLevel")
public abstract class ServerSubLevelMixin {
    @Unique
    private static final String GET_LIMIT =
            "Lnet/neoforged/neoforge/common/ModConfigSpec$DoubleValue;getAsDouble()D";

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = GET_LIMIT, ordinal = 0))
    private double genesis$noFloorInSpace(double minimum) {
        return genesis$inSpace() ? Double.NEGATIVE_INFINITY : minimum;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = GET_LIMIT, ordinal = 1))
    private double genesis$noCeilingInSpace(double maximum) {
        return genesis$inSpace() ? Double.POSITIVE_INFINITY : maximum;
    }

    @Unique
    private boolean genesis$inSpace() {
        return GenesisMod.isSpaceLikeDimension(((ServerSubLevel) (Object) this).getLevel());
    }
}
