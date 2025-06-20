package g_mungus.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import g_mungus.genesis.GenesisMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Particle.class)
public class ParticleMixin {

    @WrapMethod(method = "move")
    public void wrapMove(double d, double e, double f, Operation<Void> original) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().location().equals(GenesisMod.SPACE_DIM)) {
            original.call(d / 16, e / 16, f / 16);
        } else {
            original.call(d, e, f);
        }
    }
}
