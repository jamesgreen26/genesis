package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

import java.util.Set;
import java.util.UUID;

/// Posted on the NeoForge event bus before a Sable sub-level, with the sub-levels connected to it and the entities
/// aboard, travels between space and a celestial's dimension. Cancel it to keep them where they are; it's posted again
/// each tick they're still in range. The sub-levels' counterpart to {@link CelestialTravelEvent}.
///
/// Sable can't move a sub-level to another dimension, so each is rebuilt there as a new sub-level, with a new id, and
/// the old one removed
public class CelestialSubLevelTravelEvent extends Event implements ICancellableEvent {
    private final ServerSubLevel subLevel;
    private final Set<UUID> group;
    private final ResourceLocation celestial;
    private final CelestialTravelEvent.Direction direction;
    private final ServerLevel from;
    private final ServerLevel to;

    public CelestialSubLevelTravelEvent(ServerSubLevel subLevel, Set<UUID> group, ResourceLocation celestial,
                                        CelestialTravelEvent.Direction direction, ServerLevel from, ServerLevel to) {
        this.subLevel = subLevel;
        this.group = group;
        this.celestial = celestial;
        this.direction = direction;
        this.from = from;
        this.to = to;
    }

    /// The sub-level that reached where travel happens
    public ServerSubLevel subLevel() {
        return subLevel;
    }

    /// The ids of every sub-level travelling: {@link #subLevel()} and those connected to it, as they are before
    /// travelling
    public Set<UUID> group() {
        return group;
    }

    /// The celestial being arrived at or left, which shares its id with its dimension
    public ResourceLocation celestial() {
        return celestial;
    }

    public CelestialTravelEvent.Direction direction() {
        return direction;
    }

    public ServerLevel from() {
        return from;
    }

    public ServerLevel to() {
        return to;
    }
}
