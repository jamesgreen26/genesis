package shipwrights.genesis.space.planet_properties;

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

import java.util.ArrayList;
import java.util.function.Supplier;

@Mod.EventBusSubscriber
public record PlanetPropertiesSyncPacket(CompoundTag data) {

    public static void encode(PlanetPropertiesSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static PlanetPropertiesSyncPacket decode(FriendlyByteBuf buf) {
        return new PlanetPropertiesSyncPacket(buf.readNbt());
    }

    public static void sendToAllClients() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            MinecraftServer server = ValkyrienSkiesMod.getCurrentServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach(PlanetPropertiesSyncPacket::send);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlanetPropertiesSyncPacket.send(player);
        }
    }

    private static void send(ServerPlayer player) {
        PlanetPropertiesModel model = new PlanetPropertiesModel(
                new ArrayList<>(PlanetProperties.PLANET_PROPERTIES.values())
        );

        var data = (CompoundTag) PlanetPropertiesModel.CODEC
                .encodeStart(NbtOps.INSTANCE, model)
                .getOrThrow(false, GenesisMod.LOGGER::error);

        PlanetPropertiesSyncPacket packet = new PlanetPropertiesSyncPacket(data);
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
        public static void handle(PlanetPropertiesSyncPacket packet) {
            PlanetPropertiesModel model = PlanetPropertiesModel.CODEC
                    .parse(NbtOps.INSTANCE, packet.data)
                    .getOrThrow(false, s -> {});

            PlanetProperties.reset();
            for (PlanetProperties props : model.planets()) {
                PlanetProperties.register(props.id(), props);
            }
        }
    }
}
