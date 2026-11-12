package shipwrights.genesis.mixin;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {
    /// Shows the chunks the chunk map has been given since it last did, as it does each tick
    @Invoker("promoteChunkMap")
    boolean genesis$promoteChunkMap();
}
