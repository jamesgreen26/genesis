package shipwrights.genesis.mixin;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessAccessor {
    /// What this chunk takes its height from: its level, for one made for a level
    @Accessor("levelHeightAccessor")
    LevelHeightAccessor genesis$getLevelHeightAccessor();
}
