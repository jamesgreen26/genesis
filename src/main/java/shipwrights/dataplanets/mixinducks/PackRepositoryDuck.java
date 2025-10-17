package shipwrights.dataplanets.mixinducks;

import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public interface PackRepositoryDuck {
    Path dataplanets$getPath();
    void dataplanets$setPath(Path path);
}
