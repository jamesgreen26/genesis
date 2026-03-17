package shipwrights.genesis.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.planet_properties.PlanetPropertiesSyncPacket;
import shipwrights.genesis.space.registry.SpaceRegistrySyncPacket;
import shipwrights.genesis.space.star_properties.StarPropertiesSyncPacket;

public class GenesisNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "channel"),
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
    
    public static void init() {
        INSTANCE.messageBuilder(WormholeTravelSoundPacket.class, 0)
                .encoder(WormholeTravelSoundPacket::encode)
                .decoder(WormholeTravelSoundPacket::decode)
                .consumerMainThread(WormholeTravelSoundPacket::handle)
                .add();

        INSTANCE.messageBuilder(VoidEngineSoundPacket.class, 1)
                .encoder(VoidEngineSoundPacket::encode)
                .decoder(VoidEngineSoundPacket::decode)
                .consumerMainThread(VoidEngineSoundPacket::handle)
                .add();

        INSTANCE.messageBuilder(StopVoidEngineStartSoundPacket.class, 2)
                .encoder(StopVoidEngineStartSoundPacket::encode)
                .decoder(StopVoidEngineStartSoundPacket::decode)
                .consumerMainThread(StopVoidEngineStartSoundPacket::handle)
                .add();

        INSTANCE.messageBuilder(SpaceRegistrySyncPacket.class, 3)
                .encoder(SpaceRegistrySyncPacket::encode)
                .decoder(SpaceRegistrySyncPacket::decode)
                .consumerMainThread(SpaceRegistrySyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(PlanetPropertiesSyncPacket.class, 4)
                .encoder(PlanetPropertiesSyncPacket::encode)
                .decoder(PlanetPropertiesSyncPacket::decode)
                .consumerMainThread(PlanetPropertiesSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(StarPropertiesSyncPacket.class, 5)
                .encoder(StarPropertiesSyncPacket::encode)
                .decoder(StarPropertiesSyncPacket::decode)
                .consumerMainThread(StarPropertiesSyncPacket::handle)
                .add();
    }
}
