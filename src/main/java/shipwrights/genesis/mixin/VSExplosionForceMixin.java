package shipwrights.genesis.mixin;


import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.GenesisMod;


@Mixin(value = Explosion.class, priority = 1500)
public class VSExplosionForceMixin {

    @Shadow @Final private Level level;

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.feature.explosions.MixinExplosion",
            name = "doExplodeForce"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;mul(D)Lorg/joml/Vector3d;", ordinal = 1, remap = false)
    )
    private Vector3d genesis$scaleExplosionForce(Vector3d instance, double scalar, Operation<Vector3d> original) {
        if (GenesisMod.isMiniScale(level)) {
            return original.call(instance, scalar / 65536.0); // 16 ^ 3 for mass scaling, * 16 for distance scaling
        }
        return original.call(instance, scalar);
    }
}
