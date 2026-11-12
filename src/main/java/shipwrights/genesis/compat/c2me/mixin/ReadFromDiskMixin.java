package shipwrights.genesis.compat.c2me.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.mixin.ChunkAccessAccessor;
import shipwrights.genesis.space.VoidChunks;

/// C2ME loads chunks in place of vanilla, so a chunk that isn't on disk never starts where vanilla's would (see
/// {@link shipwrights.genesis.mixin.ChunkMapMixin}), but here, on whichever thread is loading it. Where generating it
/// would leave it empty, it starts as that instead, as it does without C2ME: see {@link VoidChunks#empty}. C2ME takes
/// it as it would a whole chunk read from disk.
///
/// Should C2ME change so that this no longer applies, space is generated, as it would be without this
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.statuses.ReadFromDisk", remap = false)
public abstract class ReadFromDiskMixin {

    @ModifyReturnValue(method = "createEmptyProtoChunk", at = @At("RETURN"), require = 0)
    private static ProtoChunk genesis$startEmptyInSpace(ProtoChunk chunk) {
        // C2ME gives its level as what the chunk takes its height from
        if (((ChunkAccessAccessor) chunk).genesis$getLevelHeightAccessor() instanceof ServerLevel level
                && VoidChunks.generatesNothing(level)) {
            return VoidChunks.empty(level, chunk.getPos());
        }
        return chunk;
    }
}
