package shipwrights.genesis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.space.client.CelestialApproachOverlay;

import java.util.function.Supplier;

/// Tells a player's client it has just travelled between space and a celestial's dimension (see
/// {@link shipwrights.genesis.space.travel.CelestialTravel}), so it can fade in from the overlay hiding the change.
/// Sent after the dimension change itself, so it's handled in the new dimension.
public record CelestialTravelledPacket() {

    public void encode(FriendlyByteBuf buffer) {
    }

    public static CelestialTravelledPacket decode(FriendlyByteBuf buffer) {
        return new CelestialTravelledPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CelestialApproachOverlay::startArrivalFade);
        context.get().setPacketHandled(true);
    }
}
