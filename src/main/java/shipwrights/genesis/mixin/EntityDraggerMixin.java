package shipwrights.genesis.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityDragger;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;

@Mixin(EntityDragger.class)
public class EntityDraggerMixin {

    @Inject(method = "dragEntitiesWithShips", at = @At("HEAD"), remap = false)
    private void onDragEntitiesWithShips(Iterable<? extends Entity> entities, boolean preTick, CallbackInfo ci) {
        entities.forEach(entity -> {
            EntityDraggingInformation info = ((IEntityDraggingInformationProvider) entity).getDraggingInformation();

            final Level level = entity.level();

            Long lastShip = info.getLastShipStoodOn();

            if (lastShip != null && info.getTicksSinceStoodOnShip() < 5 && GenesisMod.shouldCancelVoidDamage(level)) {
                final Ship ship = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(lastShip);
                if (ship != null && ship.getWorldAABB().containsPoint(VectorConversionsMCKt.toJOML(entity.position()))) {
                    info.setTicksSinceStoodOnShip(0);
                }
            }
        });
    }
}
