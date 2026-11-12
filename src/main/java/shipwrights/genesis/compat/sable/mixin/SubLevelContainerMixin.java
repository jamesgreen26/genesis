package shipwrights.genesis.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.BitSet;

/// A new sub-level's plot is put where no dimension has one, rather than only where its own has none. So a sub-level
/// travelling to another dimension finds its plot free there, and keeps it and its id (see
/// {@link shipwrights.genesis.compat.sable.SubLevelTravel}), as does everything referring to it by them: the
/// sub-levels joined to it, for one
@Mixin(targets = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer")
public abstract class SubLevelContainerMixin {

    @WrapOperation(method = "getFirstEmptyPlot", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;get(I)Z"))
    private boolean genesis$takenInAnyDimension(BitSet occupancy, int index, Operation<Boolean> original) {
        if (original.call(occupancy, index)) return true;
        if (!(((SubLevelContainer) (Object) this).getLevel() instanceof ServerLevel level)) return false;
        for (ServerLevel other : level.getServer().getAllLevels()) {
            if (other == level) continue;
            SubLevelContainer container = SubLevelContainer.getContainer(other);
            if (container != null && container.getOccupancy().get(index)) return true;
        }
        return false;
    }
}
