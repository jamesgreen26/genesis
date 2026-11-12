package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;

/// Keeps what's drifting in space drifting. Sable has its physics engine put to rest whatever has moved slower than
/// 0.15 blocks a second, and turned slower than 0.15 radians a second, for two seconds: it's stopped dead until
/// something disturbs it. That suits what has come to rest on the ground. In space, where nothing slows anything, it
/// stops a ship coasting gently towards a dock.
///
/// So each tick, every sub-level in space that's still moving at all is kept awake, which starts its two seconds
/// again. What's truly still, or near enough, is left to rest as before
final class Drift {

    /// Blocks a second, and radians a second, below which a sub-level counts as still. A sub-level left to rest at
    /// this speed would otherwise have gone 18 blocks in an hour
    private static final double STILL = 0.005;

    private Drift() {
    }

    static void init(IEventBus gameBus) {
        gameBus.addListener(Drift::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GenesisMod.isSpaceLikeDimension(level)) return;
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        Vector3d velocity = new Vector3d();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (!(subLevel instanceof ServerSubLevel server) || server.isRemoved()) continue;
            if (pipeline.getLinearVelocity(server, velocity).lengthSquared() > STILL * STILL
                    || pipeline.getAngularVelocity(server, velocity).lengthSquared() > STILL * STILL) {
                pipeline.wakeUp(server);
            }
        }
    }
}
