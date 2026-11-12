package shipwrights.genesis.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {
    /// The view distance of this chunk map's level, which every player's there is limited to
    @Accessor("viewDistance")
    int genesis$getServerViewDistance();

    /// Saves {@code chunk} if it has anything unsaved, as it would be when unloaded
    ///
    /// @return whether it was written
    @Invoker("save")
    boolean genesis$save(ChunkAccess chunk);
}
