package shipwrights.genesis.mixin.dataplanets;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.dataplanets.mixinducks.PackRepositoryDuck;

import java.nio.file.Path;

@Mixin(ServerPacksSource.class)
public class ServerPacksSourceMixin {
    @Inject(method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("TAIL"), cancellable = true)
    private static void postCreatePackRepository(Path pPath, CallbackInfoReturnable<PackRepository> cir){
        PackRepository repo = cir.getReturnValue();
        ((PackRepositoryDuck) repo).dataplanets$setPath(pPath);
        cir.setReturnValue(repo);
    }
}
