package shipwrights.genesis.tests;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;
import net.minecraft.core.Registry;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Utility for creating and positioning VS ships in GameTests.
 *
 * <p>Ship assembly calls {@link ShipAssembler#assembleToShip} directly (not via command) so the
 * returned {@link LoadedServerShip} is available immediately — no polling required to find it.
 */
public class TestShipHelper {

    private static final int MAX_ASSEMBLY_BLOCKS = 256;

    /**
     * Places a 3×3 stone platform at {@code relPos} (relative to the GameTest structure origin),
     * flood-fills the connected blocks, and assembles them into a VS ship synchronously.
     *
     * <p>The returned ship is immediately usable — no polling needed.
     */
    public static ServerShip assembleShip(GameTestHelper helper, BlockPos relPos) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(relPos);

        // Place a 3×3 stone platform for VS to assemble
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(absPos.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        List<BlockPos> blocks = collectBlocks(level, absPos);
        GenesisMod.LOGGER.info("[TestShipHelper] assembleShip: collecting {} blocks at {}", blocks.size(), absPos);

        ServerShip ship = ShipAssembler.INSTANCE.assembleToShip(level, blocks, true, 1.0, false);

        GenesisMod.LOGGER.info("[TestShipHelper] assembleShip: ship id={} slug='{}' chunkClaimDim='{}'",
                ship.getId(), ship.getSlug(), ship.getChunkClaimDimension());
        return ship;
    }

    /**
     * Teleports {@code ship} to Y = {@code atmosphereExitHeight + 20} within the same dimension
     * so that {@code PlanetToSpaceTeleporter} will pick it up on the next tick.
     */
    public static void moveShipAboveAtmosphere(ServerLevel level, ServerShip ship) {
        Vector3dc pos = ship.getTransform().getPositionInWorld();
        double targetY = GenesisCommonConfig.getAtmosphereExitHeight() + 20.0;
        String dimId = level.dimension().location().toString();
        String cmd = String.format("execute in %s run vs teleport %s %f %f %f",
                dimId, ship.getSlug(), pos.x(), targetY, pos.z());
        GenesisMod.LOGGER.info("[TestShipHelper] moveShipAboveAtmosphere: {}", cmd);
        CommandSourceStack cmdSource = level.getServer().createCommandSourceStack();
        level.getServer().getCommands().performPrefixedCommand(cmdSource, cmd);
    }

    /**
     * Teleports {@code ship} into the space dimension, positioned within the collision radius of
     * the celestial identified by {@code celestialId}.
     *
     * <p>If the celestial is not found in the space registry the method logs a warning and is a
     * no-op.
     */
    public static void moveShipNearPlanet(ServerLevel spaceLevel, ServerShip ship, ResourceLocation celestialId) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(spaceLevel);
        Celestial celestial = registry.get(celestialId);
        if (celestial == null) {
            GenesisMod.LOGGER.warn("[TestShipHelper] Celestial '{}' not found in registry; cannot position ship for planet entry test", celestialId);
            return;
        }
        long ticks = GenesisMod.getTicks(spaceLevel);
        Vector3dc celestialPos = celestial.getPosition(ticks, registry);
        // Place ship just inside the celestial's collision radius
        double targetDist = celestial.getActualSize() * 0.5;
        Vec3 targetPos = new Vec3(
                celestialPos.x(),
                celestialPos.y() + targetDist,
                celestialPos.z()
        );
        String spaceDimId = spaceLevel.dimension().location().toString();
        String cmd = String.format("execute in %s run vs teleport %s %f %f %f",
                spaceDimId, ship.getSlug(), targetPos.x, targetPos.y, targetPos.z);
        GenesisMod.LOGGER.info("[TestShipHelper] moveShipNearPlanet: celestial='{}' pos=({},{},{}) cmd={}",
                celestialId, celestialPos.x(), celestialPos.y(), celestialPos.z(), cmd);
        CommandSourceStack cmdSource = spaceLevel.getServer().createCommandSourceStack();
        spaceLevel.getServer().getCommands().performPrefixedCommand(cmdSource, cmd);
    }

    /** Flood-fills from {@code origin} collecting all 6-connected non-air blocks up to {@value MAX_ASSEMBLY_BLOCKS}. */
    private static List<BlockPos> collectBlocks(ServerLevel level, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        if (!level.getBlockState(origin).isAir()) {
            visited.add(origin);
            queue.add(origin);
        }

        while (!queue.isEmpty() && result.size() < MAX_ASSEMBLY_BLOCKS) {
            BlockPos pos = queue.poll();
            result.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (visited.add(neighbor) && !level.getBlockState(neighbor).isAir()) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }
}
