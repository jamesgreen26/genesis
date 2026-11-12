package shipwrights.genesis.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.genesis.space.VoidChunks;

/// Keeps whether each chunk is on disk: see {@link VoidChunks.OnDisk}
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements VoidChunks.OnDisk {
    /// Set by whichever thread reads the chunk, and read by the one that saves it
    @Unique
    private volatile boolean genesis$onDisk;

    @Override
    public boolean genesis$isOnDisk() {
        return genesis$onDisk;
    }

    @Override
    public void genesis$setOnDisk() {
        genesis$onDisk = true;
    }
}
