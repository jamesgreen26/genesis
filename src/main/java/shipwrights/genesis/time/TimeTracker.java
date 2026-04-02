package shipwrights.genesis.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.SyncTimeOffsetPacket;

public class TimeTracker {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;
        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;

        // doDaylightCycle is false: decrement offset each tick to cancel out gameTime++,
        // keeping genesisTime frozen. Sync every tick so clients stay in step.
        GenesisTimeData data = GenesisTimeData.getOrCreate(serverLevel.getServer());
        data.addOffset(-1);
        GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new SyncTimeOffsetPacket(data.getTimeOffset()));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long offset = GenesisTimeData.getOrCreate(server).getTimeOffset();
        GenesisNetworking.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncTimeOffsetPacket(offset));
    }
}
