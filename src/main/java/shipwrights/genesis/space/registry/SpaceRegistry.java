package shipwrights.genesis.space.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber
public class SpaceRegistry {
    private volatile Map<ResourceLocation, Celestial> celestials = new ConcurrentHashMap<>();
    private volatile Map<ResourceLocation, Celestial> pending = null;

    static final Logger LOGGER = LoggerFactory.getLogger("Expander");
    private final List<Consumer<RegisterCelestialsEvent>> registrationCallbacks;

    public SpaceRegistry(List<Consumer<RegisterCelestialsEvent>> registrationCallbacks) {
        this.registrationCallbacks = registrationCallbacks;
    }

    @Nullable public Celestial get(ResourceLocation id) {
        return celestials.get(id);
    }

    public List<Celestial> getAll() {
        return celestials.values().stream().toList();
    }

    public Collection<Celestial> getWhere(Predicate<CelestialType> predicate) {
        return celestials.values().stream().filter(a -> predicate.test(a.getType())).toList();
    }


    private void addCelestial(ResourceLocation id, Celestial it) {
        (pending != null ? pending : celestials).putIfAbsent(id, it);
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        GenesisMod.SPACE_REGISTRY.celestials.clear();
    }

    void applyClientSync(SystemConfigModel config) {
        pending = new ConcurrentHashMap<>();
        RegisterCelestialsEvent event = new RegisterCelestialsEvent(this);
        config.celestials().forEach(event::accept);
        celestials = pending;
        pending = null;
    }

    void bake() {
        pending = new ConcurrentHashMap<>();
        registrationCallbacks.forEach(it -> it.accept(new RegisterCelestialsEvent(this)));
        celestials = pending;
        pending = null;
        SpaceRegistrySyncPacket.sendToAllClients();
    }

    public static class RegisterCelestialsEvent {
        private final SpaceRegistry registry;

        private RegisterCelestialsEvent(SpaceRegistry registry) {
            this.registry = registry;
        }

        public void accept(Celestial it) {
            registry.addCelestial(it.getID(), it);
        }
    }
}
