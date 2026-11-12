package shipwrights.genesis.tests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.travel.CelestialTravel;

import java.util.UUID;

/// Travel between the overworld, which is a celestial with a dimension of its own, and space, of a pig with a chicken
/// riding it
@GameTestHolder(GenesisMod.MOD_ID)
public class CelestialTravelGameTests {

    @GameTest(template = "emptyplatform")
    public static void leavesForSpaceWithRider(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);

        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        Chicken chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(1, 2, 1));
        chicken.startRiding(pig, true);
        pig.moveTo(pig.getX(), GenesisCommonConfig.atmosphereExitHeight() + 5, pig.getZ());
        // Slowly, so it's still near where it arrives when it's checked, a tick or two later
        pig.setDeltaMovement(0, 0.2, 0);
        UUID pigId = pig.getUUID();
        UUID chickenId = chicken.getUUID();

        helper.succeedWhen(() -> {
            Entity arrived = space.getEntity(pigId);
            helper.assertTrue(arrived != null, "pig didn't arrive in space");
            Entity rider = space.getEntity(chickenId);
            helper.assertTrue(rider != null && rider.getVehicle() == arrived, "chicken isn't riding the pig in space");
            helper.assertTrue(overworld.getEntity(pigId) == null, "pig is still in the overworld");

            OBB overworldCube = cube(space, Level.OVERWORLD.location());
            Vector3d position = new Vector3d(arrived.getX(), arrived.getY(), arrived.getZ());
            double distance = overworldCube.closestPointTo(position).distance(position);
            helper.assertTrue(distance > CelestialTravel.DEPARTURE_DISTANCE - 0.5
                            && distance < CelestialTravel.DEPARTURE_DISTANCE + 2,
                    "pig arrived " + distance + " blocks from the overworld");

            // Up, which it was moving, is out from the overworld's surface in space
            Vector3d outward = new Vector3d(position).sub(overworldCube.closestPointTo(position)).normalize();
            Vec3 velocity = arrived.getDeltaMovement();
            helper.assertTrue(new Vector3d(velocity.x, velocity.y, velocity.z).normalize().dot(outward) > 0.9,
                    "pig isn't moving away from the overworld: " + velocity);

            rider.discard();
            arrived.discard();
        });
    }

    @GameTest(template = "emptyplatform")
    public static void arrivesFromSpaceWithRider(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel space = space(helper);

        // Just off the centre of the overworld's north face, which is the origin of the overworld, moving towards it
        var celestials = space.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Celestial celestial = celestials.getOptional(Level.OVERWORLD.location()).orElseThrow();
        Time time = Time.get(space);
        Vector3dc centre = celestial.transformProvider().getPosition(celestials, time);
        Quaterniondc rotation = celestial.transformProvider().getRotation(celestials, time);
        Vector3d start = rotation.transform(new Vector3d(0, 0, -(celestial.size() / 2 + 4))).add(centre);
        Vector3d inward = rotation.transform(new Vector3d(0, 0, 0.5));

        ChunkPos chunk = new ChunkPos(BlockPos.containing(start.x, start.y, start.z));
        space.setChunkForced(chunk.x, chunk.z, true);
        Pig pig = EntityType.PIG.create(space);
        Chicken chicken = EntityType.CHICKEN.create(space);
        helper.assertTrue(pig != null && chicken != null, "couldn't create the pig and chicken");
        pig.setNoAi(true);
        chicken.setNoAi(true);
        pig.moveTo(start.x, start.y, start.z);
        chicken.moveTo(start.x, start.y, start.z);
        pig.setDeltaMovement(inward.x, inward.y, inward.z);
        space.addFreshEntity(pig);
        space.addFreshEntity(chicken);
        chicken.startRiding(pig, true);
        UUID pigId = pig.getUUID();
        UUID chickenId = chicken.getUUID();

        helper.succeedWhen(() -> {
            Entity arrived = overworld.getEntity(pigId);
            helper.assertTrue(arrived != null, "pig didn't arrive in the overworld");
            Entity rider = overworld.getEntity(chickenId);
            helper.assertTrue(rider != null && rider.getVehicle() == arrived, "chicken isn't riding the pig in the overworld");

            // Within a few blocks on the cube, as the overworld moves on while the pig's chunk loads
            double blocksPerCubeBlock = GenesisCommonConfig.worldSize() / 8.0 / (celestial.size() / 2);
            double tolerance = 3 * blocksPerCubeBlock;
            helper.assertTrue(Math.abs(arrived.getX()) < tolerance && Math.abs(arrived.getZ()) < tolerance,
                    "pig arrived at " + arrived.position() + ", not near the origin");
            helper.assertTrue(Math.abs(arrived.getY() - GenesisCommonConfig.atmosphereEntryHeight()) < 5,
                    "pig arrived at height " + arrived.getY());

            // Towards the surface, which it was moving, is down in the overworld
            Vec3 velocity = arrived.getDeltaMovement();
            helper.assertTrue(velocity.y < -0.3 && Math.abs(velocity.x) < 0.05 && Math.abs(velocity.z) < 0.05,
                    "pig isn't moving down: " + velocity);

            rider.discard();
            arrived.discard();
            space.setChunkForced(chunk.x, chunk.z, false);
        });
    }

    private static ServerLevel space(GameTestHelper helper) {
        ServerLevel space = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE));
        helper.assertTrue(space != null, "no space dimension");
        return space;
    }

    private static OBB cube(ServerLevel space, ResourceLocation id) {
        var celestials = space.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Celestial celestial = celestials.getOptional(id).orElseThrow();
        Time time = Time.get(space);
        return OBB.createCube(celestial.size(), celestial.transformProvider().getRotation(celestials, time),
                celestial.transformProvider().getPosition(celestials, time));
    }
}
