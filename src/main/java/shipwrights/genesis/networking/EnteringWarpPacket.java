package shipwrights.genesis.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.client.ClientStorage;
import shipwrights.genesis.client.WarpLoadingMenu;

import java.util.function.Supplier;

public class EnteringWarpPacket {

    public static void encode(EnteringWarpPacket msg, FriendlyByteBuf buf) {}

    public static EnteringWarpPacket decode(FriendlyByteBuf buf) {
        return new EnteringWarpPacket();
    }

    public static void handle(EnteringWarpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientStorage.goingToFromWormhole = true;
        });
        ctx.get().setPacketHandled(true);
    }
}
