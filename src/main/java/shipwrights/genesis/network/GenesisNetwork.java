package shipwrights.genesis.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import shipwrights.genesis.GenesisMod;

public final class GenesisNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            GenesisMod.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private GenesisNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(CelestialTravelPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CelestialTravelPacket::encode)
                .decoder(CelestialTravelPacket::decode)
                .consumerMainThread(CelestialTravelPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
