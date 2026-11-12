package shipwrights.genesis.compat.vs;

import org.valkyrienskies.core.api.events.PhysTickEvent;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// Which ships are joined by Valkyrien Skies joints (hinges, ropes and so on), so they travel together. Joints are only
/// known to the physics thread, so each physics tick publishes them for the game thread to read.
///
/// A joint with only one ship holds it to the world: such ships are {@linkplain #ANCHOR anchored}.
final class ShipConnections {

    /// Stands for the world, in what a ship is joined to
    static final long ANCHOR = Long.MIN_VALUE;

    /// For each dimension, each ship joined to others there, with what it's joined to. Replaced whole for a dimension
    /// each physics tick there
    private static final Map<String, Map<Long, Set<Long>>> CONNECTIONS = new ConcurrentHashMap<>();

    private ShipConnections() {
    }

    static void onPhysTick(PhysTickEvent event) {
        if (!(event.getWorld() instanceof VsiPhysLevel level)) return;

        Map<Long, Set<Long>> joined = new HashMap<>();
        for (VSJoint joint : level.getAllJoints().values()) {
            Long a = joint.getShipId0();
            Long b = joint.getShipId1();
            if (a == null && b == null) continue;
            long first = a == null ? ANCHOR : a;
            long second = b == null ? ANCHOR : b;
            joined.computeIfAbsent(first, id -> new HashSet<>()).add(second);
            joined.computeIfAbsent(second, id -> new HashSet<>()).add(first);
        }

        Map<Long, Set<Long>> published = new HashMap<>();
        joined.forEach((id, others) -> published.put(id, Set.copyOf(others)));
        CONNECTIONS.put(level.getDimension(), Map.copyOf(published));
    }

    /// {@code shipId} and every ship joined to it, directly or through others, first, and {@link #ANCHOR} if any of
    /// them is held to the world
    static Set<Long> group(String dimension, long shipId, Collection<Long> groundBodyIds) {
        Map<Long, Set<Long>> current = CONNECTIONS.getOrDefault(dimension, Map.of());
        Set<Long> group = new LinkedHashSet<>();
        Deque<Long> toVisit = new ArrayDeque<>();
        toVisit.add(shipId);
        while (!toVisit.isEmpty()) {
            long id = toVisit.poll();
            if (groundBodyIds.contains(id)) id = ANCHOR;
            if (!group.add(id) || id == ANCHOR) continue;
            toVisit.addAll(current.getOrDefault(id, Set.of()));
        }
        return group;
    }
}
