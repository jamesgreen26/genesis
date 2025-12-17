package shipwrights.genesis.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record SyncPlanetsPacket(List<PlanetEntry> entries) {

    public record PlanetEntry(
            ResourceLocation dimensionID,
            ResourceLocation parentDimensionID,
            double size,
            double orbitRadius,
            double yearLength,
            float r,
            float g,
            float b,
            double gravity
    ) {}

    public static SyncPlanetsPacket fromPlanets(Collection<PlanetData> planets) {
        List<PlanetEntry> entries = new ArrayList<>();
        for (PlanetData planet : planets) {
            int[] rgb = PlanetData.floatToRgb(planet.color);
            entries.add(new PlanetEntry(
                    planet.dimensionID,
                    planet.parent != null ? planet.parent.dimensionID : null,
                    planet.getActualSize() / GenesisMod.earthSize,
                    planet.orbitRadius,
                    (double) planet.getYearLengthTicks() / GenesisMod.earthYear,
                    rgb[0] / 255f,
                    rgb[1] / 255f,
                    rgb[2] / 255f,
                    planet.gravity
            ));
        }
        return new SyncPlanetsPacket(entries);
    }

    public static void encode(SyncPlanetsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entries.size());
        for (PlanetEntry entry : packet.entries) {
            buf.writeResourceLocation(entry.dimensionID);
            buf.writeBoolean(entry.parentDimensionID != null);
            if (entry.parentDimensionID != null) {
                buf.writeResourceLocation(entry.parentDimensionID);
            }
            buf.writeDouble(entry.size);
            buf.writeDouble(entry.orbitRadius);
            buf.writeDouble(entry.yearLength);
            buf.writeFloat(entry.r);
            buf.writeFloat(entry.g);
            buf.writeFloat(entry.b);
            buf.writeDouble(entry.gravity);
        }
    }

    public static SyncPlanetsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<PlanetEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation dimensionID = buf.readResourceLocation();
            ResourceLocation parentDimensionID = buf.readBoolean() ? buf.readResourceLocation() : null;
            double size = buf.readDouble();
            double orbitRadius = buf.readDouble();
            double yearLength = buf.readDouble();
            float r = buf.readFloat();
            float g = buf.readFloat();
            float b = buf.readFloat();
            double gravity = buf.readDouble();
            entries.add(new PlanetEntry(dimensionID, parentDimensionID, size, orbitRadius, yearLength, r, g, b, gravity));
        }
        return new SyncPlanetsPacket(entries);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientHandler.handle(this));
        }
        context.setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(SyncPlanetsPacket packet) {
            GenesisMod.planets.clear();

            // First pass: create planets without parents
            List<PlanetEntry> moonsToProcess = new ArrayList<>();

            for (PlanetEntry entry : packet.entries) {
                if (entry.parentDimensionID == null) {
                    PlanetData planet = new PlanetData(
                            entry.dimensionID,
                            null,
                            entry.size,
                            entry.orbitRadius,
                            entry.yearLength,
                            entry.r,
                            entry.g,
                            entry.b,
                            entry.gravity
                    );
                    GenesisMod.planets.put(entry.dimensionID, planet);
                } else {
                    moonsToProcess.add(entry);
                }
            }

            // Second pass: create moons with parent references
            for (PlanetEntry entry : moonsToProcess) {
                PlanetData parent = GenesisMod.planets.get(entry.parentDimensionID);
                PlanetData moon = new PlanetData(
                        entry.dimensionID,
                        parent,
                        entry.size,
                        entry.orbitRadius,
                        entry.yearLength,
                        entry.r,
                        entry.g,
                        entry.b,
                        entry.gravity
                );
                GenesisMod.planets.put(entry.dimensionID, moon);
            }

            GenesisMod.LOGGER.info("Synced {} planets from server", GenesisMod.planets.size());
        }
    }
}
