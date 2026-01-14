package shipwrights.genesis.space.registry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.GenesisNetworking;

import java.util.function.Supplier;

@Mod.EventBusSubscriber
public record SpaceRegistrySyncPacket(CompoundTag data) {

    public static void encode(SpaceRegistrySyncPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SpaceRegistrySyncPacket decode(FriendlyByteBuf buf) {
        return new SpaceRegistrySyncPacket(buf.readNbt());
    }

    public static void sendToAllClients() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            MinecraftServer server = ValkyrienSkiesMod.getCurrentServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach(SpaceRegistrySyncPacket::send);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpaceRegistrySyncPacket.send(player);
        }
    }

    private static void send(ServerPlayer player) {
        SystemConfigModel config = new SystemConfigModel(
                GenesisMod.SPACE_REGISTRY.getAll()
        );

        var data = (CompoundTag) SystemConfigModel.CODEC
                .encodeStart(NbtOps.INSTANCE, config)
                .getOrThrow(false, GenesisMod.LOGGER::error);

        SpaceRegistrySyncPacket packet = new SpaceRegistrySyncPacket(data);
        GenesisNetworking.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                packet
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientHandler.handle(this));
        }
        context.setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(SpaceRegistrySyncPacket packet) {
            SystemConfigModel config = SystemConfigModel.CODEC
                    .parse(NbtOps.INSTANCE, packet.data)
                    .getOrThrow(false, s -> {});

            GenesisMod.SPACE_REGISTRY.applyClientSync(config);
        }
    }
}
