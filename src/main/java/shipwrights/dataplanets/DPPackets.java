package shipwrights.dataplanets;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DPPackets {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("dataplanets", "channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final PacketDistributor<SimpleChannel> ALL = new PacketDistributor<>(
            (distributor, channelGetter) -> packet -> ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList()
                    .getPlayers()
                    .forEach(player -> player.connection.connection.send(packet)),
            NetworkDirection.PLAY_TO_CLIENT);

    public static <PACKET> void sendToAll(SimpleChannel channel, PACKET packet)
    {
        channel.send(ALL.with(()->channel), packet);
    }

}
