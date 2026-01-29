package shipwrights.genesis.teleportation;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static void moveShipToDimensionAt(ServerLevel from, ServerShip ship, String dimensionID, Vec3 pos)
    {
        CommandSourceStack stack = from.getServer().createCommandSourceStack();
        Vector3dc joml = ship.getTransform().getPositionInWorld();
        Vec3 fromPos = new Vec3(joml.x(),joml.y(),joml.z());
        AABBdc box = ship.getWorldAABB();
        List<Entity> allToMove = from.getEntitiesOfClass(Entity.class,new AABB(new Vec3(box.minX(),box.minY(),box.minZ()),new Vec3(box.maxX(),box.maxY(),box.maxZ())));

        for(Entity e: allToMove)
        {
            Vec3 relative = fromPos.subtract(e.getPosition(0));
            Vec3 end = pos.add(relative);

            from.getServer().getCommands().performPrefixedCommand(stack,
                    "execute in "+dimensionID+" run tp "+e.getStringUUID()+" "+end.x()+" "+end.y()+" "+end.z());
        }

        from.getServer().getCommands().performPrefixedCommand(stack,
                "execute in "+dimensionID+" run vs teleport "+ship.getSlug()+" "+pos.x()+" "+pos.y()+" "+pos.z());

    }
}
