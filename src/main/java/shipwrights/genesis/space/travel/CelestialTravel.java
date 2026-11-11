package shipwrights.genesis.space.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.math.OBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;

/// Moves entities between space and the dimensions of celestials, along with everything riding them.
///
/// In space, anything that comes within {@value #ARRIVAL_DISTANCE} blocks of a celestial with a dimension of its own,
/// sharing its id, goes to that dimension, at {@linkplain GenesisCommonConfig#atmosphereEntryHeight() the entry height}.
/// In a celestial's dimension, anything above {@linkplain GenesisCommonConfig#atmosphereExitHeight() the exit height}
/// goes to space, {@value #DEPARTURE_DISTANCE} blocks out from the celestial's surface. Where on the celestial is given
/// by {@link CelestialProjection}, stretched to {@linkplain GenesisCommonConfig#worldSize() the world size}.
///
/// Velocity and facing turn with the frame of the surface where they cross, relative to the celestial, so that up in
/// its dimension is out from its surface. What can travel is what could go through a nether portal, but riding or
/// ridden: see {@link #canTravel}.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class CelestialTravel {

    /// How close to a celestial's surface something in space has to come to arrive at it
    public static final double ARRIVAL_DISTANCE = 8;

    /// How far from a celestial's surface what leaves it is placed in space. Further than {@link #ARRIVAL_DISTANCE},
    /// so it isn't sent straight back
    public static final double DEPARTURE_DISTANCE = 12;

    /// While true, entities ignore riding and being ridden in deciding whether they can change dimensions (see
    /// {@link shipwrights.genesis.mixin.EntityMixin})
    private static final ThreadLocal<Boolean> CHECKING_STACK = ThreadLocal.withInitial(() -> false);

    /// Where each entity moved by its client was at the end of the last tick, as its server doesn't know its velocity
    private static final Map<Entity, LastPosition> LAST_POSITIONS = new WeakHashMap<>();

    private CelestialTravel() {
    }

    public static boolean isCheckingStack() {
        return CHECKING_STACK.get();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        List<Entity> roots = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!entity.isPassenger()) roots.add(entity);
        }

        if (GenesisMod.isSpaceDimension(level)) {
            tickSpace(level, roots);
        } else {
            Optional<Celestial> celestial = GenesisMod.celestial(level);
            celestial.ifPresent(c -> tickCelestial(level, c, roots));
        }

        for (Entity root : roots) {
            if (!root.isRemoved() && root.level() == level && isMovedByClient(root)) {
                LAST_POSITIONS.put(root, new LastPosition(level.dimension(), level.getGameTime(), root.position()));
            }
        }
    }

    private static void tickSpace(ServerLevel space, List<Entity> roots) {
        Registry<Celestial> celestials = space.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Time time = Time.get(space);

        List<Destination> destinations = new ArrayList<>();
        for (var entry : celestials.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            ServerLevel level = space.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level == null || level == space) continue;
            destinations.add(new Destination(id, level, Placement.of(entry.getValue(), celestials, time)));
        }
        if (destinations.isEmpty()) return;

        for (Entity root : roots) {
            if (root.isRemoved() || root.isPassenger()) continue;
            Vec3 position = root.position();
            Vector3d point = new Vector3d(position.x, position.y, position.z);
            for (Destination destination : destinations) {
                if (destination.placement().obb().closestPointTo(point).distance(point) < ARRIVAL_DISTANCE) {
                    arrive(root, space, destination, point);
                    break;
                }
            }
        }
    }

    private static void arrive(Entity root, ServerLevel space, Destination destination, Vector3dc point) {
        Placement placement = destination.placement();
        Vector3d local = placement.rotation().conjugate(new Quaterniond())
                .transform(new Vector3d(point).sub(placement.position()));
        Vector2d map = CelestialProjection.toMap(local);

        double scale = mapScale();
        // Never at or above the exit height, which would send it straight back
        double y = Math.min(GenesisCommonConfig.atmosphereEntryHeight(), GenesisCommonConfig.atmosphereExitHeight() - 1);
        Vec3 target = new Vec3(map.x * scale, y, map.y * scale);
        Quaterniond toDimension = new Quaterniond(placement.rotation())
                .mul(CelestialProjection.frame(map.x, map.y))
                .conjugate();

        travel(root, destination.id(), CelestialTravelEvent.Direction.TO_CELESTIAL, space, destination.level(), target,
                toDimension);
    }

    private static void tickCelestial(ServerLevel level, Celestial celestial, List<Entity> roots) {
        ServerLevel space = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE));
        if (space == null) return;

        int exitHeight = GenesisCommonConfig.atmosphereExitHeight();
        Placement placement = null;
        for (Entity root : roots) {
            if (root.isRemoved() || root.isPassenger() || root.getY() <= exitHeight) continue;
            if (placement == null) {
                placement = Placement.of(celestial, space.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY),
                        Time.get(space));
            }
            leave(root, level, space, celestial, placement);
        }
    }

    private static void leave(Entity root, ServerLevel level, ServerLevel space, Celestial celestial, Placement placement) {
        double scale = mapScale();
        double mapX = root.getX() / scale;
        double mapZ = root.getZ() / scale;

        Quaterniond frame = CelestialProjection.frame(mapX, mapZ);
        Vector3d local = CelestialProjection.toCube(mapX, mapZ).mul(celestial.size() / 2)
                .add(frame.transform(new Vector3d(0, DEPARTURE_DISTANCE, 0)));
        Vector3d target = placement.rotation().transform(local).add(placement.position());
        Quaterniond toSpace = new Quaterniond(placement.rotation()).mul(frame);

        travel(root, level.dimension().location(), CelestialTravelEvent.Direction.TO_SPACE, level, space,
                new Vec3(target.x, target.y, target.z), toSpace);
    }

    /// Blocks in a celestial's dimension per unit of {@link CelestialProjection}'s map
    private static double mapScale() {
        return GenesisCommonConfig.worldSize() / (2 * CelestialProjection.HALF_WIDTH);
    }

    /// @param rotation from the frame of {@code from} to that of {@code to}, which velocity and facing are turned by
    private static void travel(Entity root, ResourceLocation celestial, CelestialTravelEvent.Direction direction,
                               ServerLevel from, ServerLevel to, Vec3 position, Quaterniondc rotation) {
        if (!canTravel(root)) return;
        if (MinecraftForge.EVENT_BUS.post(new CelestialTravelEvent(root, celestial, direction, from, to))) return;

        Vec3 velocity = rotate(velocity(root, from), rotation);

        // Keep the arrival loaded, as a nether portal does, so what arrives alone carries on moving
        BlockPos block = BlockPos.containing(position);
        to.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(block), 3, block);

        LAST_POSITIONS.remove(root);
        teleportStack(root, to, position, velocity, rotation);
    }

    /// Whether {@code root} and everything riding it could each go through a nether portal, but for riding or being
    /// ridden, which vanilla doesn't allow: so not ender dragons, withers, fishing hooks or sleeping mobs. Nor
    /// spectators, which nether portals never touch
    private static boolean canTravel(Entity root) {
        if (root.getSelfAndPassengers().anyMatch(Entity::isSpectator)) return false;
        CHECKING_STACK.set(true);
        try {
            return root.getSelfAndPassengers().allMatch(Entity::canChangeDimensions);
        } finally {
            CHECKING_STACK.set(false);
        }
    }

    /// Moves {@code entity} and what rides it, all to {@code position}, and seats each again on what it rode
    private static Entity teleportStack(Entity entity, ServerLevel to, Vec3 position, Vec3 velocity, Quaterniondc rotation) {
        List<Entity> passengers = List.copyOf(entity.getPassengers());
        // Vanilla only ejects passengers for entities other than players, after it's decided to move them
        entity.ejectPassengers();

        float[] look = rotateLook(entity.getYRot(), entity.getXRot(), rotation);
        Entity moved = entity.changeDimension(to, new Teleporter(position, velocity, look[0], look[1]));
        if (moved == null) {
            for (Entity passenger : passengers) {
                passenger.startRiding(entity, true);
            }
            return null;
        }

        for (Entity passenger : passengers) {
            Entity movedPassenger = teleportStack(passenger, to, position, velocity, rotation);
            if (movedPassenger != null) {
                movedPassenger.startRiding(moved, true);
            }
        }
        return moved;
    }

    /// Whether its client moves {@code root}, and the server only learns where it ends up
    private static boolean isMovedByClient(Entity root) {
        return root instanceof Player || root.getControllingPassenger() instanceof Player;
    }

    /// Blocks per tick. The server doesn't know the velocity of what its client moves, so that's taken from how far it
    /// moved over the last tick
    private static Vec3 velocity(Entity root, ServerLevel level) {
        if (isMovedByClient(root)) {
            LastPosition last = LAST_POSITIONS.get(root);
            if (last != null && last.level() == level.dimension() && last.gameTime() == level.getGameTime() - 1) {
                return root.position().subtract(last.position());
            }
        }
        return root.getDeltaMovement();
    }

    private static Vec3 rotate(Vec3 vector, Quaterniondc rotation) {
        Vector3d rotated = rotation.transform(new Vector3d(vector.x, vector.y, vector.z));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    /// Turns a facing by {@code rotation}, as Minecraft's yaw and pitch in degrees: `{yRot, xRot}`
    static float[] rotateLook(float yRot, float xRot, Quaterniondc rotation) {
        double yaw = Math.toRadians(yRot);
        double pitch = Math.toRadians(xRot);
        Vector3d look = rotation.transform(new Vector3d(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        ));
        float newXRot = (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -look.y))));
        float newYRot = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        return new float[]{newYRot, newXRot};
    }

    /// Where a celestial is in space, and how it's turned
    private record Placement(Vector3dc position, Quaterniondc rotation, OBB obb) {
        static Placement of(Celestial celestial, Registry<Celestial> celestials, Time time) {
            Vector3dc position = celestial.transformProvider().getPosition(celestials, time);
            Quaterniondc rotation = celestial.transformProvider().getRotation(celestials, time);
            return new Placement(position, rotation, OBB.createCube(celestial.size(), rotation, position));
        }
    }

    private record Destination(ResourceLocation id, ServerLevel level, Placement placement) {
    }

    private record LastPosition(ResourceKey<Level> level, long gameTime, Vec3 position) {
    }

    /// Places what travels exactly where it's sent, moving and facing as given
    private record Teleporter(Vec3 position, Vec3 velocity, float yRot, float xRot) implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(position, velocity, yRot, xRot);
        }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                  Function<Boolean, Entity> repositionEntity) {
            Entity placed = repositionEntity.apply(false);
            if (placed != null) {
                // Vanilla keeps the pitch of entities other than players, and sets velocity only for those
                placed.setXRot(xRot);
                placed.setYHeadRot(yRot);
                placed.setYBodyRot(yRot);
                placed.setDeltaMovement(velocity);
                // Sends the velocity to clients, including to the client of a player, which moves it
                placed.hurtMarked = true;
            }
            return placed;
        }

        @Override
        public boolean isVanilla() {
            return false;
        }

        @Override
        public boolean playTeleportSound(net.minecraft.server.level.ServerPlayer player, ServerLevel sourceWorld,
                                         ServerLevel destWorld) {
            return false;
        }
    }
}
