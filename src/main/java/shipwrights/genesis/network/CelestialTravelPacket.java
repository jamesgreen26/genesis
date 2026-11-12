package shipwrights.genesis.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.client.CelestialApproachOverlay;

/// Tells a player's client it's about to travel between space and a celestial's dimension (see
/// {@link shipwrights.genesis.space.travel.CelestialTravel}), so it can hide the change of dimension behind the overlay
/// rather than vanilla's loading screens. Sent just before the dimension change, so it's handled first.
public record CelestialTravelPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CelestialTravelPacket> TYPE =
            new CustomPacketPayload.Type<>(GenesisMod.id("celestial_travel"));
    public static final StreamCodec<ByteBuf, CelestialTravelPacket> STREAM_CODEC =
            StreamCodec.unit(new CelestialTravelPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /// Only registered to be handled on the client
    public static void handle(CelestialTravelPacket packet, IPayloadContext context) {
        context.enqueueWork(CelestialApproachOverlay::startTravel);
    }
}
