package shipwrights.genesis.mixin.dataplanets;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.dataplanets.mixinducks.PackRepositoryDuck;

import java.nio.file.Path;

@Mixin(PackRepository.class)
public class PackRepositoryMixin implements PackRepositoryDuck {
    @Unique
    public LevelStorageSource.LevelStorageAccess everyMansSky$levelStorageAccess;

    @Unique
    public Path everyMansSky$path;

    @Override
    public Path dataplanets$getPath() {
        return everyMansSky$path;
    }

    @Override
    public void dataplanets$setPath(Path path) {
        everyMansSky$path = path;
    }
}
