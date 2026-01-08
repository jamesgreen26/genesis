package shipwrights.genesis.space.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Orbitable;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.Star;

import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class SpaceRegistry {
    private final Map<ResourceLocation, OrbitingBody> orbitingBodies = new HashMap<>();
    private final Map<ResourceLocation, OrbitingBody> registryQueue = new HashMap<>();
    private final Map<ResourceLocation, Star> stars = new HashMap<>();

    static final Logger LOGGER = LoggerFactory.getLogger("Expander");
    private final List<Consumer<RegisterCelestialsEvent>> registrationCallbacks;

    public SpaceRegistry(List<Consumer<RegisterCelestialsEvent>> registrationCallbacks) {
        this.registrationCallbacks = registrationCallbacks;
    }

    public boolean isRegistered(Orbitable orbitable) {
        return getStar(orbitable.getID()) == orbitable || getOrbitingBody(orbitable.getID()) == orbitable;
    }

    @Nullable public Orbitable get(ResourceLocation id) {
        Star star = getStar(id);
        if (star != null) { return star; }
        return getOrbitingBody(id);
    }

    @Nullable public Star getStar(ResourceLocation ID) {
        return stars.get(ID);
    }

    @Nullable public OrbitingBody getOrbitingBody(ResourceLocation ID) {
        return orbitingBodies.get(ID);
    }

    public List<Star> getAllStars() {
        return stars.values().stream().toList();
    }

    public List<OrbitingBody> getAllOrbitingBodies() {
        return orbitingBodies.values().stream().toList();
    }

    private void addStar(ResourceLocation id, Star star) {
        stars.putIfAbsent(id, star);
    }

    private void addOrbitingBody(ResourceLocation id, OrbitingBody body) {
        registryQueue.putIfAbsent(id, body);
    }


    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        GenesisMod.SPACE_REGISTRY.reset();
    }

    void reset() {
        orbitingBodies.clear();
        stars.clear();
        registryQueue.clear();
    }

    void applyClientSync(SystemConfigModel config) {
        reset();
        RegisterCelestialsEvent event = new RegisterCelestialsEvent(this);
        config.stars().forEach(star -> event.accept(star.getID(), star));
        config.bodies().forEach(body -> event.accept(body.getID(), body));
        // Don't run callbacks on client - just link the parents from server data
        linkParents();
    }

    void bake() {
        registrationCallbacks.forEach(it -> it.accept(new RegisterCelestialsEvent(this)));
        linkParents();
        SpaceRegistrySyncPacket.sendToAllClients();
    }

    private void linkParents() {
        boolean progress;
        do {
            progress = false;
            for (var entry : List.copyOf(registryQueue.entrySet())) {
                ResourceLocation id = entry.getKey();
                OrbitingBody body = entry.getValue();
                Orbitable parent = get(body.getParentID());
                if (parent != null) {
                    body.defineParent(parent);
                    orbitingBodies.putIfAbsent(id, body);
                    registryQueue.remove(id);
                    progress = true;
                }
            }
        } while (progress);

        for (var entry : registryQueue.entrySet()) {
            ResourceLocation id = entry.getKey();
            OrbitingBody body = entry.getValue();
            LOGGER.warn("Failed to register celestial {} due to missing parent: {}", id, body.getParentID());
        }

        registryQueue.clear();
    }

    public static class RegisterCelestialsEvent {
        private final SpaceRegistry registry;

        private RegisterCelestialsEvent(SpaceRegistry registry) {
            this.registry = registry;
        }

        public void accept(ResourceLocation id, Orbitable it) {
            if (it instanceof Star star) {
                registry.addStar(id, star);
            } else if (it instanceof OrbitingBody body) {
                registry.addOrbitingBody(id, body);
            }
        }
    }
}
