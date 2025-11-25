package shipwrights.genesis.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.block.RadarDisplayBlock;
import shipwrights.genesis.radar.RadarDisplay;

import java.util.ArrayList;
import java.util.List;

public class RadarDisplayBlockEntity extends BlockEntity {

    public RadarDisplay display = new RadarDisplay(20);

    public RadarDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.RADAR_DISPLAY.get(), pos, state);
    }

    public double[][] getDisplayableData() {
        return display.data;
    }

    public void clientTick() {
        if (level == null) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.getBlock() instanceof RadarDisplayBlock) {
            Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos());
            Vec3i normalShip = state.getValue(RadarDisplayBlock.FACING).getNormal();

            Vector3d pos;
            Vector3d dir;
            List<Long> excludedShips = new ArrayList<>(1);
            pos = VectorConversionsMCKt.toJOML(getBlockPos().getCenter());
            dir = new Vector3d(normalShip.getX(), normalShip.getY(), normalShip.getZ());

            if (ship != null) {
                pos = ship.getShipToWorld().transformPosition(pos);
                dir = ship.getShipToWorld().transformDirection(dir);
                excludedShips.add(ship.getId());
            }

            display.scan(level, pos, dir, excludedShips);
        }
    }
}
