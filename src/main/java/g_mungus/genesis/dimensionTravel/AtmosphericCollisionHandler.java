package g_mungus.genesis.dimensionTravel;

import g_mungus.genesis.GenesisMod;
import g_mungus.genesis.PlanetRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.*;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

@EventBusSubscriber
public class AtmosphericCollisionHandler {


    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        ServerLevel space = event.getServer().getLevel(ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), GenesisMod.SPACE_DIM));
        space.setChunkForced(0,0, true);
    }

    @SubscribeEvent
    public static void handleShipsEnteringSpace(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        PlanetRegistry.PlanetData data = PlanetRegistry.getPlanetData(level.dimension().location());
        if (data == null) return;

        VSGameUtilsKt.getAllShips(level).forEach(ship -> {
           if (ship.getTransform().getPositionInWorld().y() >= GenesisMod.atmosphereCollisionHeight) {
               moveShipToSpace((ServerShip) ship, (ServerLevel) level, data);
           }
        });
    }

    private static void moveShipToSpace(ServerShip ship, ServerLevel fromLevel, PlanetRegistry.PlanetData data) {

        Vector3dc newPos = new Vector3d(data.location().x() + data.size() + 2, data.location().y(), data.location().z());

        ServerLevel destLevel = getLevel(GenesisMod.SPACE_DIM);

        ShipTeleportDataImpl teleportData = new ShipTeleportDataImpl(
                newPos,
                new Quaterniond(),
                new Vector3d(),
                new Vector3d(),
                "minecraft:dimension:" + GenesisMod.SPACE_DIM,
                1/16.0,
                null
        );

        if (destLevel != null) {
            VSGameUtilsKt.getShipObjectWorld(fromLevel).teleportShip(ship, teleportData);
        }
    }

    public static ServerLevel getLevel(ResourceLocation dimensionID) {
        MinecraftServer server = ValkyrienSkiesMod.getCurrentServer();

        if (server != null) {
            ResourceKey<Level> levelId = ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), dimensionID);
            return server.getLevel(levelId);
        } else {
            return null;
        }
    }
}
