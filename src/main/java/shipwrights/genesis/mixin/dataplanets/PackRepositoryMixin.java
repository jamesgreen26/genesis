package shipwrights.genesis.mixin.dataplanets;


import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.dataplanets.mixinducks.PackRepositoryDuck;

import java.nio.file.Path;

@Mixin(PackRepository.class)
public class PackRepositoryMixin implements PackRepositoryDuck {
    @Unique
    public Path dataplanets$path;

    @Override
    public Path dataplanets$getPath() {
        return dataplanets$path;
    }

    public void dataplanets$setPath(Path path) {
        dataplanets$path = path;
    }
}
