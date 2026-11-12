package shipwrights.genesis.compat.c2me.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.space.VoidChunks;

/// C2ME saves a chunk as it's unloaded here, off the server's thread, rather than where vanilla does (see
/// {@link shipwrights.genesis.mixin.ChunkMapMixin}), having first asked whether it has anything unsaved. A chunk of
/// space holding nothing hasn't, unless one is on disk in its place: see {@link VoidChunks#leaveUnsaved}.
///
/// Should C2ME change so that this no longer applies, empty space is saved, as it would be without this
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.statuses.ReadFromDiskAsync", remap = false)
public abstract class ReadFromDiskAsyncMixin {

    @WrapOperation(method = "asyncSave", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;isUnsaved()Z"), require = 0)
    private boolean genesis$leaveVoidUnsaved(ChunkAccess chunk, Operation<Boolean> original) {
        if (!original.call(chunk)) return false;
        if (VoidChunks.leaveUnsaved(chunk)) {
            chunk.setUnsaved(false);
            return false;
        }
        return true;
    }
}
