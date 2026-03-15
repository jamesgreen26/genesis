package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.GenesisMod;

@Mixin(FlameParticle.class)
public class FlameParticleMixin {

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"))
    public AABB wrapMove(AABB instance, double p_82387_, double p_82388_, double p_82389_, Operation<AABB> original) {
        ClientLevel level = Minecraft.getInstance().level;
        double scale = 1.0;
        if (level != null) {
            scale = GenesisMod.getDimensionScale(level);
        }

        return original.call(instance,  p_82387_ * scale, p_82388_ * scale, p_82389_ * scale);
    }
}
