package shipwrights.genesis.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.GenesisMod;

import java.util.function.Supplier;

public class SyncTimeOffsetPacket {

    public final long timeOffset;

    public SyncTimeOffsetPacket(long timeOffset) {
        this.timeOffset = timeOffset;
    }

    public static void encode(SyncTimeOffsetPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.timeOffset);
    }

    public static SyncTimeOffsetPacket decode(FriendlyByteBuf buf) {
        return new SyncTimeOffsetPacket(buf.readLong());
    }

    public static void handle(SyncTimeOffsetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> GenesisMod.clientTimeOffset = msg.timeOffset);
        ctx.get().setPacketHandled(true);
    }
}
