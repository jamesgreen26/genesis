package shipwrights.genesis.mixin;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheInvoker {
    /// Forgets the last few chunks found, as it does each tick, so they're found again
    @Invoker("clearCache")
    void genesis$clearCache();
}
