package shipwrights.genesis.compat.vs.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.entity.handling.WorldEntityHandler;

/// Backports Valkyrien Skies' fix for riders unseated on the client after ship travel between dimensions
/// (ValkyrienSkies/Valkyrien-Skies-2#2036). Drop once the Valkyrien Skies Genesis needs has it. Nothing in
/// {@code genesis.vs.mixins.json} is required, so should Valkyrien Skies change past where this can apply, it's
/// skipped rather than keeping the game from loading
@Mixin(value = WorldEntityHandler.class, remap = false)
public abstract class WorldEntityHandlerMixin {

    /// No ship owns the vehicle's place in the shipyard. On the server that means the ship is gone, so the rider is put
    /// off rather than left in the shipyard. On a client it can also mean the ship hasn't arrived yet: after a ship
    /// changes dimension, a seat on it reaches the client before the ship does. Dismounting there would desync the
    /// client, as the server still has the rider seated and so never tells it to mount again. Leave the rider where it
    /// is, and leave dismounting to the server
    @WrapWithCondition(method = "positionSetFromVehicle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;stopRiding()V", remap = true))
    private boolean genesis$onlyServerUnseats(Entity self) {
        return !self.level().isClientSide;
    }
}
