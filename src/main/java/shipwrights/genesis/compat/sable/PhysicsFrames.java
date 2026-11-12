package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.object.ArbitraryPhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.sable.mixin.Rapier3DInvoker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/// Has Sable's physics engine keep the sub-levels of space near its own origin, wherever in space they are.
///
/// The engine keeps positions as 32-bit floats, and moves everything 720 times a second. Far from its origin a float
/// is too coarse for steps that small: 15,000 blocks out, where the overworld is, nothing can move slower than a third
/// of a block a second, and at 60,000, slower than a block and a half. Within a thousand blocks, anything faster than
/// a fiftieth of a block a second can, and the nearer the finer: within a hundred, speeds come in steps of a
/// two-hundredth of a block a second.
///
/// So sub-levels that are near each other, or joined, share a frame: a place near the engine's origin that it's told
/// they're around, rather than where they are. What it's told of where a sub-level is, and what it tells, is moved by
/// its frame's {@linkplain Frame#offset offset} on the way (see
/// {@link shipwrights.genesis.compat.sable.mixin.RapierPhysicsPipelineMixin}). Nothing else knows: sub-levels are
/// where they were, to Sable and to everything that asks it. Frames are kept apart in the engine, further than
/// anything in them could reach in a tick, so what's in one never touches what's in another there. The first
/// frames have the places nearest the engine's origin, a few dozen within a couple of hundred blocks of it, and so
/// the finest movement.
///
/// Each tick, between the engine's steps:
///
/// - sub-levels in different frames that have come within {@value #LINK_NEAR} blocks of each other, or been joined,
///   are put in one, before they could touch;
/// - those sharing a frame that are more than {@value #LINK_FAR} blocks apart, and not joined, are given their own;
/// - a frame whose sub-levels have gone {@value #RECENTRE} blocks from where the engine has them is brought back.
///
/// Each of these moves sub-levels in the engine by a whole number of blocks, and not at all in the level.
///
/// A joint that holds a sub-level to the world, rather than to another, is told of as where it holds it in the
/// sub-level's frame, and told again each time that changes (see {@link #worldJoint}). Where what uses such a joint
/// drives it by positions of its own, it's left to tell the engine all of them in the frame (see
/// {@link #localJoints}).
///
/// The engine holds more besides: ropes, boxes, and hinges to the world. Those it's told of as where they are, which
/// can't be changed afterwards. So they keep to the world's frame, as everything did before, and so does any
/// sub-level near them or held by them, for as long as it is. There they're no worse off than they were.
///
/// Only where the engine has no terrain, which is also where it is: in space, with Sable told to ignore its chunks
public final class PhysicsFrames {

    /// How close sub-levels in different frames come before they share one
    static final double LINK_NEAR = 48;

    /// How far apart sub-levels sharing a frame go, unjoined, before they stop sharing it
    static final double LINK_FAR = 96;

    /// How far a frame's sub-levels go from where the engine has them before the frame is brought back
    static final double RECENTRE = 32;

    /// How far a sub-level is moved at once, as by a command, before it's taken to have left what it was near
    private static final double FAR_MOVE = 256;

    /// Frames are given places in the engine on a grid this far apart, nearest its origin first
    private static final double CELL = 64;

    /// How far a new place for a frame is from everything in every other frame: far enough that two frames can each
    /// go as far as they do before they're brought back, and still not be given other places
    private static final double CELL_GAP = 112;

    /// How close what's in two frames may come in the engine before one of them is given another place: further than
    /// anything closes in a tick, at any speed the engine allows
    private static final double CONFLICT_GAP = 32;

    /// How many places out from the engine's origin are looked through for a free one
    private static final int MAX_RING = 48;

    private static final Vector3dc ZERO = new Vector3d();

    private static final Map<ServerLevel, PhysicsFrames> BY_LEVEL = new WeakHashMap<>();

    /// By the engine's own handle for each level's scene, which is all some of what it's told comes with
    private static final Long2ObjectOpenHashMap<PhysicsFrames> BY_SCENE = new Long2ObjectOpenHashMap<>();

    /// What holds a sub-level to the world's frame, by whatever it is that holds it and which part of it does
    private static final Map<Object, Map<Object, Pin>> PINS = new WeakHashMap<>();

    /// The joints that hold sub-levels to the world, each by Sable's own handle for it
    private static final Map<Object, WorldJoint> WORLD_JOINTS = new WeakHashMap<>();

    /// The joint to the world the engine has just been told of, until Sable has made its handle for it
    private static WorldJoint pendingWorldJoint;

    /// Whether what's making joints now tells the engine where they are in their sub-levels' frames itself
    private static boolean localJoints;

    private final ServerLevel level;

    /// Where the world is: the frame of everything the engine can't be told has moved
    private final Frame world = new Frame(true);

    /// Each sub-level's frame, by its id in the engine
    private final Int2ObjectOpenHashMap<Frame> frames = new Int2ObjectOpenHashMap<>();

    /// Where each sub-level last was in the level, by its id in the engine
    private final Int2ObjectOpenHashMap<AABBd> bounds = new Int2ObjectOpenHashMap<>();

    /// How many things hold each sub-level to the world's frame, by its id in the engine
    private final Int2IntOpenHashMap pinned = new Int2IntOpenHashMap();

    /// The joints holding each sub-level to the world, by its id in the engine
    private final Int2ObjectOpenHashMap<List<WorldJoint>> worldJoints = new Int2ObjectOpenHashMap<>();

    /// Whether sub-levels are being moved in the engine without moving in the level
    private boolean shifting;

    private PhysicsFrames(ServerLevel level) {
        this.level = level;
    }

    static void init(IEventBus gameBus) {
        gameBus.addListener(PhysicsFrames::onLevelTick);
        gameBus.addListener(PhysicsFrames::onLevelUnload);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PhysicsFrames frames = BY_LEVEL.get(level);
            if (frames != null) frames.tick();
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PhysicsFrames frames = BY_LEVEL.remove(level);
            if (frames != null) BY_SCENE.values().removeIf(other -> other == frames);
        }
    }

    /// The frames of {@code level}, if its sub-levels are kept in them
    private static PhysicsFrames of(ServerLevel level) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null && GenesisMod.isSpaceLikeDimension(level) && DimensionPhysicsData.of(level).ignoreChunks()) {
            frames = new PhysicsFrames(level);
            BY_LEVEL.put(level, frames);
        }
        return frames;
    }

    // What the engine is told, and tells, of sub-levels

    /// A sub-level is being made in the engine, at a position in {@code level}
    ///
    /// @return how far the engine is to be told it is from there
    public static Vector3dc created(ServerLevel level, long scene, int id, double x, double y, double z) {
        PhysicsFrames frames = of(level);
        if (frames == null) return ZERO;
        BY_SCENE.put(scene, frames);
        return frames.place(id, new Vector3d(x, y, z)).offset;
    }

    /// @return how far the sub-level is, in {@code level}, from where the engine has it
    public static Vector3dc offset(ServerLevel level, int id) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null) return ZERO;
        Frame frame = frames.frames.get(id);
        return frame == null ? ZERO : frame.offset;
    }

    /// A sub-level is being put at a position in {@code level}
    ///
    /// @return how far the engine is to be told it is from there
    public static Vector3dc teleported(ServerLevel level, int id, double x, double y, double z) {
        PhysicsFrames frames = of(level);
        if (frames == null) return ZERO;
        Frame frame = frames.frames.get(id);
        Vector3d position = new Vector3d(x, y, z);
        if (frame == null) return frames.place(id, position).offset;
        if (frames.shifting) return frame.offset;

        AABBd last = frames.bounds.get(id);
        if (last != null && distance(last, position) > FAR_MOVE) {
            // It's left whatever it was near, and may have arrived near something else
            frames.frames.remove(id);
            frames.bounds.remove(id);
            return frames.place(id, position).offset;
        }
        return frame.offset;
    }

    public static void removed(ServerLevel level, int id) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null) return;
        frames.frames.remove(id);
        frames.bounds.remove(id);
        frames.pinned.remove(id);
        frames.worldJoints.remove(id);
    }

    // What the engine is told of the rest, which keeps to the world's frame

    /// Something other than a sub-level is being made in the engine, within {@code box} of its level: every sub-level
    /// near enough to touch it joins it in the world's frame first
    public static void worldObject(long scene, AABBd box) {
        PhysicsFrames frames = BY_SCENE.get(scene);
        if (frames != null) frames.toWorld(box);
    }

    /// {@code first} and {@code second} are being joined: they share a frame first, rather than by the next tick.
    /// Nothing to do if either is null, and the other is being held to the world
    public static void joining(ServerLevel level, PhysicsPipelineBody first, PhysicsPipelineBody second) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null || first == null || second == null) return;
        Frame a = frames.frames.get(first.getRuntimeId());
        Frame b = frames.frames.get(second.getRuntimeId());
        if (a == null || b == null || a == b) return;
        if (a.world) {
            frames.merge(b, a);
        } else {
            frames.merge(a, b);
        }
    }

    /// {@code body} is about to be held to the world by something the engine can't be told has moved: it joins the
    /// world's frame first, with all that share its own
    public static void toWorldFrame(ServerLevel level, PhysicsPipelineBody body) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null || body == null || localJoints) return;
        Frame frame = frames.frames.get(body.getRuntimeId());
        if (frame != null && !frame.world) frames.merge(frame, frames.world);
    }

    /// Whether the engine is to be told where a joint holds a sub-level of {@code level} to the world in that
    /// sub-level's frame, by {@link #worldJoint}
    public static boolean movesWorldJoints(ServerLevel level) {
        return !localJoints && BY_LEVEL.get(level) != null;
    }

    /// The engine has been told of {@code joint}, which holds the sub-level {@code body} to the world at a position
    /// in {@code level}, turned by a quaternion: on its {@code side}, the first being 0. It's told where that is in
    /// the sub-level's frame from now on
    public static void worldJoint(ServerLevel level, long scene, long joint, int body, int side,
                                  double x, double y, double z, double qx, double qy, double qz, double qw) {
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null) return;
        pendingWorldJoint = new WorldJoint(frames, scene, joint, body, side, new Vector3d(x, y, z),
                new double[]{qx, qy, qz, qw});
    }

    /// Sable has made {@code handle} for the joint that holds {@code body} to the world: one it was just
    /// {@linkplain #worldJoint told of in the sub-level's frame}, or else one that holds it to the world's frame
    /// until it's {@linkplain #unpin let go}
    public static void heldToWorld(ServerLevel level, PhysicsPipelineBody body, Object handle) {
        WorldJoint joint = pendingWorldJoint;
        pendingWorldJoint = null;
        if (localJoints || handle == null) return;
        if (joint == null) {
            pin(level, body, handle, "");
            return;
        }
        WORLD_JOINTS.put(handle, joint);
        joint.frames.worldJoints.computeIfAbsent(joint.body, id -> new ArrayList<>()).add(joint);
    }

    /// The joint Sable has {@code handle} for is being told where, on its {@code side}, it holds what it does. If
    /// that's where it holds a sub-level to the world, it's a position in the level
    ///
    /// @return how far the engine is to be told it is from there
    public static Vector3dc worldJointMoved(Object handle, int side, double x, double y, double z,
                                            double qx, double qy, double qz, double qw) {
        WorldJoint joint = WORLD_JOINTS.get(handle);
        if (joint == null || joint.side != side) return ZERO;
        joint.at.set(x, y, z);
        joint.rotation[0] = qx;
        joint.rotation[1] = qy;
        joint.rotation[2] = qz;
        joint.rotation[3] = qw;
        Frame frame = joint.frames.frames.get(joint.body);
        return frame == null ? ZERO : frame.offset;
    }

    /// Has whatever makes joints to the world within {@code local} tell the engine where they are itself: at the
    /// origin of a sub-level's frame, say, with what drives them measured from there too
    ///
    /// @return whether that was so already, to be put back by another call once done
    public static boolean localJoints(boolean local) {
        boolean was = localJoints;
        localJoints = local;
        return was;
    }

    /// {@code body} is held to the world's frame by {@code slot} of {@code owner}, until that's
    /// {@linkplain #unpin let go}
    public static void pin(ServerLevel level, PhysicsPipelineBody body, Object owner, Object slot) {
        unpin(owner, slot);
        PhysicsFrames frames = BY_LEVEL.get(level);
        if (frames == null || body == null || owner == null) return;
        int id = body.getRuntimeId();
        PINS.computeIfAbsent(owner, key -> new HashMap<>()).put(slot, new Pin(frames, id));
        frames.pinned.addTo(id, 1);
        Frame frame = frames.frames.get(id);
        if (frame != null && !frame.world) frames.merge(frame, frames.world);
    }

    public static void unpin(Object owner, Object slot) {
        Map<Object, Pin> pins = PINS.get(owner);
        if (pins == null) return;
        Pin pin = pins.remove(slot);
        if (pin != null && pin.frames.pinned.addTo(pin.id, -1) <= 1) pin.frames.pinned.remove(pin.id);
    }

    /// Lets go of everything {@code owner} holds, and forgets it if it's a joint to the world
    public static void unpin(Object owner) {
        WorldJoint joint = WORLD_JOINTS.remove(owner);
        if (joint != null) {
            List<WorldJoint> joints = joint.frames.worldJoints.get(joint.body);
            if (joints != null) joints.remove(joint);
        }
        Map<Object, Pin> pins = PINS.get(owner);
        if (pins == null) return;
        for (Object slot : List.copyOf(pins.keySet())) unpin(owner, slot);
    }

    // For tests

    /// Where the engine has {@code subLevel}
    static Vector3d inEngine(ServerSubLevel subLevel) {
        return new Vector3d(subLevel.logicalPose().position()).sub(offset(subLevel.getLevel(), subLevel.getRuntimeId()));
    }

    static boolean shareFrame(ServerSubLevel first, ServerSubLevel second) {
        PhysicsFrames frames = BY_LEVEL.get(first.getLevel());
        return frames == null || frames.frames.get(first.getRuntimeId()) == frames.frames.get(second.getRuntimeId());
    }

    static boolean inWorldFrame(ServerSubLevel subLevel) {
        PhysicsFrames frames = BY_LEVEL.get(subLevel.getLevel());
        if (frames == null) return true;
        Frame frame = frames.frames.get(subLevel.getRuntimeId());
        return frame == null || frame.world;
    }

    // Frames

    /// The frame for a sub-level newly at {@code position}: that of whatever it's near, or one of its own
    private Frame place(int id, Vector3d position) {
        Frame frame = null;
        double nearest = LINK_NEAR;
        for (var entry : bounds.int2ObjectEntrySet()) {
            double distance = distance(entry.getValue(), position);
            Frame other = frames.get(entry.getIntKey());
            if (distance < nearest && other != null) {
                nearest = distance;
                frame = other;
            }
        }

        AABBd box = new AABBd(position, position);
        if (frame == null) {
            frame = new Frame(false);
            Vector3d anchor = new Vector3d(position).round();
            frame.cell.set(allocate(box, anchor, occupied(null)));
            frame.offset.set(anchor).sub(frame.cell);
        }
        frames.put(id, frame);
        bounds.put(id, box);
        return frame;
    }

    /// Everything in {@code from} joins {@code into}
    private void merge(Frame from, Frame into) {
        List<ServerSubLevel> moving = new ArrayList<>();
        for (ServerSubLevel subLevel : subLevels()) {
            if (frames.get(subLevel.getRuntimeId()) == from) moving.add(subLevel);
        }
        shift(moving, () -> {
            for (ServerSubLevel subLevel : moving) frames.put(subLevel.getRuntimeId(), into);
        });
    }

    /// Every sub-level near {@code box}, and all that share its frame, joins the world's frame
    private void toWorld(AABBd box) {
        List<Frame> near = new ArrayList<>();
        for (var entry : bounds.int2ObjectEntrySet()) {
            Frame frame = frames.get(entry.getIntKey());
            if (frame != null && !frame.world && !near.contains(frame) && gap(entry.getValue(), box) < LINK_NEAR) {
                near.add(frame);
            }
        }
        for (Frame frame : near) merge(frame, world);
    }

    /// Moves {@code subLevels} in the engine as {@code change} to their frames has it, leaving each where it is in
    /// the level, turned and moving as it was
    private void shift(List<ServerSubLevel> subLevels, Runnable change) {
        if (subLevels.isEmpty()) {
            change.run();
            return;
        }
        PhysicsPipeline pipeline = SubLevelContainer.getContainer(level).physicsSystem().getPipeline();
        List<Pose3d> poses = new ArrayList<>(subLevels.size());
        for (ServerSubLevel subLevel : subLevels) poses.add(pipeline.readPose(subLevel, new Pose3d()));
        change.run();
        shifting = true;
        try {
            for (int i = 0; i < subLevels.size(); i++) {
                // The engine keeps how it's moving
                pipeline.teleport(subLevels.get(i), poses.get(i).position(), poses.get(i).orientation());
            }
        } finally {
            shifting = false;
        }
        // And what holds them to the world holds them where it did in the level
        for (ServerSubLevel subLevel : subLevels) {
            int id = subLevel.getRuntimeId();
            List<WorldJoint> joints = worldJoints.get(id);
            Frame frame = frames.get(id);
            if (joints == null || frame == null) continue;
            for (WorldJoint joint : joints) {
                Rapier3DInvoker.genesis$setConstraintFrame(joint.scene, joint.joint, joint.side,
                        joint.at.x - frame.offset.x, joint.at.y - frame.offset.y, joint.at.z - frame.offset.z,
                        joint.rotation[0], joint.rotation[1], joint.rotation[2], joint.rotation[3]);
            }
        }
    }

    private List<ServerSubLevel> subLevels() {
        List<ServerSubLevel> subLevels = new ArrayList<>();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return subLevels;
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel server && !server.isRemoved()) subLevels.add(server);
        }
        return subLevels;
    }

    private void tick() {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;

        List<ServerSubLevel> subLevels = subLevels();
        int count = subLevels.size();
        Map<SubLevel, Integer> indices = new IdentityHashMap<>();
        AABBd[] boxes = new AABBd[count];
        Frame[] current = new Frame[count];
        Int2ObjectOpenHashMap<AABBd> seen = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            ServerSubLevel subLevel = subLevels.get(i);
            int id = subLevel.getRuntimeId();
            indices.put(subLevel, i);
            boxes[i] = box(subLevel);
            current[i] = frames.get(id);
            if (current[i] == null) current[i] = place(id, boxes[i].center(new Vector3d()));
            seen.put(id, boxes[i]);
        }
        // Whatever went without the engine being told
        frames.keySet().retainAll(seen.keySet());
        pinned.keySet().retainAll(seen.keySet());
        worldJoints.keySet().retainAll(seen.keySet());
        bounds.clear();
        bounds.putAll(seen);
        if (count == 0) return;

        // Sub-levels that have to share a frame: near each other, or joined
        int[] parents = new int[count];
        for (int i = 0; i < count; i++) parents[i] = i;
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                if (current[i].world && current[j].world) continue;
                double limit = current[i] == current[j] ? LINK_FAR : LINK_NEAR;
                if (gap(boxes[i], boxes[j]) < limit) union(parents, i, j);
            }
            Collection<SubLevel> connected = SubLevelHelper.getConnectedChain(subLevels.get(i));
            if (connected.size() > 1) {
                for (SubLevel other : connected) {
                    Integer j = indices.get(other);
                    if (j != null) union(parents, i, j);
                }
            }
        }

        // And those that have to share the world's: held to it, or near what's in it that isn't a sub-level
        List<AABBd> objects = new ArrayList<>();
        BoundingBox3d objectBox = new BoundingBox3d();
        for (ArbitraryPhysicsObject object : container.physicsSystem().getArbitraryObjects()) {
            object.getBoundingBox(objectBox);
            objects.add(new AABBd(objectBox.minX(), objectBox.minY(), objectBox.minZ(),
                    objectBox.maxX(), objectBox.maxY(), objectBox.maxZ()));
        }
        Map<Integer, Group> groups = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Group group = groups.computeIfAbsent(find(parents, i), root -> new Group());
            group.members.add(i);
            group.box.union(boxes[i]);
            if (pinned.get(subLevels.get(i).getRuntimeId()) > 0) group.world = true;
            double limit = current[i].world ? LINK_FAR : LINK_NEAR;
            for (AABBd object : objects) {
                if (gap(boxes[i], object) < limit) group.world = true;
            }
        }

        // The biggest first, so that it's the smallest that are moved out of the way of the rest
        List<Group> ordered = new ArrayList<>(groups.values());
        ordered.sort(Comparator.comparingInt((Group group) -> group.members.size()).reversed());

        // Where everything is in the engine, as it's settled: first, what's in the world's frame
        List<AABBd> occupied = new ArrayList<>();
        for (Group group : ordered) {
            if (!group.world) continue;
            group.frame = world;
            for (int i : group.members) occupied.add(boxes[i]);
        }

        List<Frame> taken = new ArrayList<>();
        for (Group group : ordered) {
            if (group.world) continue;

            // The frame most of it is in already, unless a bigger group has it
            Map<Frame, Integer> votes = new IdentityHashMap<>();
            for (int i : group.members) {
                if (!current[i].world && !taken.contains(current[i])) votes.merge(current[i], 1, Integer::sum);
            }
            Frame frame = null;
            for (var vote : votes.entrySet()) {
                if (frame == null || vote.getValue() > votes.get(frame)) frame = vote.getKey();
            }

            Vector3d centre = group.box.center(new Vector3d());
            Vector3d anchor = new Vector3d(centre).round();
            Vector3d cell = null;
            Vector3d offset = null;
            if (frame != null) {
                // Left as it is, if it's still near where the engine has it and nothing else has come near there
                Vector3d inEngine = new Vector3d(centre).sub(frame.offset).sub(frame.cell);
                boolean near = Math.max(Math.abs(inEngine.x), Math.max(Math.abs(inEngine.y), Math.abs(inEngine.z))) <= RECENTRE;
                if (near && free(moved(group.box, frame.offset), occupied, CONFLICT_GAP)) {
                    cell = frame.cell;
                    offset = frame.offset;
                } else {
                    // Brought back to the same place, if that's still free
                    Vector3d back = new Vector3d(anchor).sub(frame.cell);
                    if (free(moved(group.box, back), occupied, CONFLICT_GAP)) {
                        cell = frame.cell;
                        offset = back;
                    }
                }
            } else {
                frame = new Frame(false);
            }
            if (cell == null) {
                cell = allocate(group.box, anchor, occupied);
                offset = new Vector3d(anchor).sub(cell);
            }
            taken.add(frame);
            group.frame = frame;
            group.cell = cell;
            group.offset = offset;
            occupied.add(moved(group.box, offset));
        }

        // Then everything whose frame has changed, or has moved, is moved in the engine, all at once
        List<ServerSubLevel> moving = new ArrayList<>();
        for (Group group : ordered) {
            boolean frameMoved = !group.world && !group.frame.offset.equals(group.offset);
            for (int i : group.members) {
                if (current[i] != group.frame || frameMoved) moving.add(subLevels.get(i));
            }
        }
        shift(moving, () -> {
            for (Group group : ordered) {
                if (!group.world) {
                    group.frame.cell.set(group.cell);
                    group.frame.offset.set(group.offset);
                }
                for (int i : group.members) frames.put(subLevels.get(i).getRuntimeId(), group.frame);
            }
        });
    }

    /// Where everything is in the engine, but for what's in {@code except}
    private List<AABBd> occupied(Frame except) {
        List<AABBd> occupied = new ArrayList<>();
        for (var entry : bounds.int2ObjectEntrySet()) {
            Frame frame = frames.get(entry.getIntKey());
            if (frame != null && frame != except) occupied.add(moved(entry.getValue(), frame.offset));
        }
        return occupied;
    }

    /// A place in the engine for what's within {@code box} of the level, about {@code anchor} there: the nearest to
    /// the engine's origin that's {@value #CELL_GAP} blocks clear of everything in {@code occupied}
    private static Vector3d allocate(AABBd box, Vector3dc anchor, List<AABBd> occupied) {
        Vector3d cell = new Vector3d();
        Vector3d offset = new Vector3d();
        for (int ring = 0; ring <= MAX_RING; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int y = -ring; y <= ring; y++) {
                    for (int z = -ring; z <= ring; z++) {
                        if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) != ring) continue;
                        cell.set(x, y, z).mul(CELL);
                        if (free(moved(box, offset.set(anchor).sub(cell)), occupied, CELL_GAP)) return cell;
                    }
                }
            }
        }
        // Nowhere near is clear. Further out, where it's still better off than where it is in the level
        return cell.set(MAX_RING + 1 + occupied.size(), 0, 0).mul(CELL);
    }

    /// Where {@code subLevel} is in the level: its blocks, or if it has none yet, the point it's at
    private static AABBd box(ServerSubLevel subLevel) {
        if (subLevel.getPlot().getBoundingBox() == BoundingBox3i.EMPTY) {
            Vector3dc position = subLevel.logicalPose().position();
            return new AABBd(position, position);
        }
        BoundingBox3dc box = subLevel.boundingBox();
        return new AABBd(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    /// {@code box} of the level, as the engine has it in a frame at {@code offset}
    private static AABBd moved(AABBd box, Vector3dc offset) {
        return new AABBd(box.minX - offset.x(), box.minY - offset.y(), box.minZ - offset.z(),
                box.maxX - offset.x(), box.maxY - offset.y(), box.maxZ - offset.z());
    }

    private static boolean free(AABBd box, List<AABBd> occupied, double clearance) {
        for (AABBd other : occupied) {
            if (gap(box, other) < clearance) return false;
        }
        return true;
    }

    /// How far apart two boxes are along the axis they're furthest apart on, which is no more than how far apart
    /// they are. Negative if they overlap
    private static double gap(AABBd a, AABBd b) {
        double x = Math.max(a.minX - b.maxX, b.minX - a.maxX);
        double y = Math.max(a.minY - b.maxY, b.minY - a.maxY);
        double z = Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ);
        return Math.max(x, Math.max(y, z));
    }

    private static double distance(AABBd box, Vector3dc point) {
        return gap(box, new AABBd(point, point));
    }

    private static int find(int[] parents, int i) {
        while (parents[i] != i) {
            parents[i] = parents[parents[i]];
            i = parents[i];
        }
        return i;
    }

    private static void union(int[] parents, int a, int b) {
        parents[find(parents, a)] = find(parents, b);
    }

    /// A place near the engine's origin, and the sub-levels it has around there
    private static final class Frame {
        /// The world's frame, where the engine has everything where it is in the level
        final boolean world;

        /// Where in the engine its sub-levels are kept around
        final Vector3d cell = new Vector3d();

        /// How far each of its sub-levels is, in the level, from where the engine has it
        final Vector3d offset = new Vector3d();

        Frame(boolean world) {
            this.world = world;
        }
    }

    /// Sub-levels that have to share a frame this tick, by their places among all of them, and the frame they're to
    /// share
    private static final class Group {
        final List<Integer> members = new ArrayList<>();
        final AABBd box = new AABBd();
        boolean world;
        Frame frame;
        Vector3d cell;
        Vector3d offset;
    }

    private record Pin(PhysicsFrames frames, int id) {
    }

    /// A joint holding the sub-level {@code body} to the world {@code at} a position in its level, turned by
    /// {@code rotation}, on its {@code side}: by the engine's handles for the level's {@code scene} and the
    /// {@code joint}
    private record WorldJoint(PhysicsFrames frames, long scene, long joint, int body, int side, Vector3d at,
                              double[] rotation) {
    }
}
