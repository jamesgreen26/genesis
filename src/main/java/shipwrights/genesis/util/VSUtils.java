package shipwrights.genesis.util;

import net.minecraft.server.level.ServerLevel;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class VSUtils {
    public static List<LoadedServerShip> getLoadedShipsInLevel(ServerLevel level) {
        final String dimId = VSGameUtilsKt.getDimensionId(level);
        final List<LoadedServerShip> loadedShips = new ArrayList<>();
        final QueryableShipData<LoadedServerShip> allShips = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips();

        allShips.forEach(ship -> {
            if (dimId.equals(ship.getChunkClaimDimension())) {
                loadedShips.add(ship);
            }
        });
        return loadedShips;
    }
}
