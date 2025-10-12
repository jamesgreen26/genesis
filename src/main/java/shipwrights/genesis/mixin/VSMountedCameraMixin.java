package shipwrights.genesis.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import shipwrights.genesis.GenesisMod;


@Mixin(value = Camera.class, priority = 1500)
public class VSMountedCameraMixin {

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.client.MixinCamera",
            name = "setupWithShipMounted"
    )
    @ModifyVariable(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "STORE",
                    ordinal = 1
            )
    )
    private double genesis$scaleShipCameraDistance(double original) {
        Level level = Minecraft.getInstance().level;
        if (level != null && GenesisMod.isMiniScale(level)) {
            return original / 12.0;
        } else {
            return original;
        }
    }
}
