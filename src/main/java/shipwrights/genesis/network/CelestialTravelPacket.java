package shipwrights.genesis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.space.client.CelestialApproachOverlay;

import java.util.function.Supplier;

/// Tells a player's client it's about to travel between space and a celestial's dimension (see
/// {@link shipwrights.genesis.space.travel.CelestialTravel}), so it can hide the change of dimension behind the overlay
/// rather than vanilla's loading screens. Sent just before the dimension change, so it's handled first.
public record CelestialTravelPacket() {

    public void encode(FriendlyByteBuf buffer) {
    }

    public static CelestialTravelPacket decode(FriendlyByteBuf buffer) {
        return new CelestialTravelPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CelestialApproachOverlay::startTravel);
        context.get().setPacketHandled(true);
    }
}
