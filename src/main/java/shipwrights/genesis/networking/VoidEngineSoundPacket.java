package shipwrights.genesis.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.client.WormholeAmbianceHandler;

import java.util.function.Supplier;

public record VoidEngineSoundPacket(BlockPos enginePos) {

    public static void encode(VoidEngineSoundPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.enginePos);
    }

    public static VoidEngineSoundPacket decode(FriendlyByteBuf buf) {
        return new VoidEngineSoundPacket(buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientHandler.handle(this));
        }
        context.setPacketHandled(true);
    }

    private static class ClientHandler {

        public static void handle(VoidEngineSoundPacket packet) {
            WormholeAmbianceHandler.voidEngineStartPos = packet.enginePos;
            WormholeAmbianceHandler.playVoidEngineStart();
        }
    }
}
