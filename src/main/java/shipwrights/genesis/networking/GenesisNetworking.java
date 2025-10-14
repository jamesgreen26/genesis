package shipwrights.genesis.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import shipwrights.genesis.GenesisMod;

import java.util.function.Supplier;

public class GenesisNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(GenesisMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

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
