package shipwrights.genesis.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.Map;
import java.util.Set;

@Mixin(ShipObjectServerWorld.class)
public interface ServerShipObjectWorldAccessor {
    @Accessor(value = "shipIdToConstraints", remap = false)
    Map<Long, Set<Integer>> getShipIdToConstraints();

    @Accessor(value = "constraints", remap = false)
    Map<Integer, VSConstraint> getConstraints();
}
