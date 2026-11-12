package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.space.VoidChunks;

/// Keeps the chunks of space cheap: see {@link VoidChunks}
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    ServerLevel level;

    /// Whether this level {@linkplain VoidChunks#generatesNothing generates nothing}, once that's been found
    @Unique
    private Boolean genesis$generatesNothing;

    /// A chunk that isn't on disk starts here, as one with nothing generated yet. Where generating it would leave it
    /// empty, it starts as that instead
    @ModifyReturnValue(method = "createEmptyChunk", at = @At("RETURN"))
    private ChunkAccess genesis$startEmptyInSpace(ChunkAccess chunk, @Local(argsOnly = true) ChunkPos pos) {
        if (genesis$generatesNothing == null) genesis$generatesNothing = VoidChunks.generatesNothing(level);
        return genesis$generatesNothing ? VoidChunks.empty(level, pos) : chunk;
    }

    /// Each level has a view distance of its own, set from the server's as it starts and whenever that changes. Every
    /// player's is limited to it, and so is how far away each is shown entities
    @ModifyVariable(method = "setViewDistance", at = @At("HEAD"), argsOnly = true)
    private int genesis$limitViewDistanceInSpace(int viewDistance) {
        return VoidChunks.viewDistance(level, viewDistance);
    }

    /// Every chunk is saved through here, as it's unloaded and as the level is saved. A chunk of space holding nothing
    /// isn't, unless one is on disk in its place
    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("HEAD"), cancellable = true)
    private void genesis$leaveVoidUnsaved(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (chunk.isUnsaved() && VoidChunks.leaveUnsaved(chunk)) {
            chunk.setUnsaved(false);
            cir.setReturnValue(false);
        }
    }
}
