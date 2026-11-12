package shipwrights.genesis.space.travel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

/// Posted on the NeoForge event bus before an entity, with everything riding it, travels between space and a celestial's
/// dimension. Cancel it to keep them where they are; it's posted again each tick they're still in range.
///
/// Each entity also goes through NeoForge's {@link net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent} as it's
/// moved. If that's cancelled for one of the passengers, it's left behind, dismounted.
public class CelestialTravelEvent extends Event implements ICancellableEvent {
    private final Entity rootVehicle;
    private final ResourceLocation celestial;
    private final Direction direction;
    private final ServerLevel from;
    private final ServerLevel to;

    public CelestialTravelEvent(Entity rootVehicle, ResourceLocation celestial, Direction direction, ServerLevel from,
                                ServerLevel to) {
        this.rootVehicle = rootVehicle;
        this.celestial = celestial;
        this.direction = direction;
        this.from = from;
        this.to = to;
    }

    /// The entity everything travelling rides, which isn't riding anything itself
    public Entity rootVehicle() {
        return rootVehicle;
    }

    /// The celestial being arrived at or left, which shares its id with its dimension
    public ResourceLocation celestial() {
        return celestial;
    }

    public Direction direction() {
        return direction;
    }

    public ServerLevel from() {
        return from;
    }

    public ServerLevel to() {
        return to;
    }

    public enum Direction {
        TO_CELESTIAL,
        TO_SPACE
    }
}
