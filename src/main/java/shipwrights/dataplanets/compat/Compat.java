package shipwrights.dataplanets.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import shipwrights.dataplanets.systemCreation.PlanetData;

import java.util.List;

public interface Compat {
    default void addPlanetsToSpace(MinecraftServer server, List<PlanetData> planets) {}

    default ResourceLocation getSpaceDimensionEffects() {
        return ResourceLocation.withDefaultNamespace("the_end");
    }

    static Compat get() {
        boolean isGenesLoaded = ModList.get().isLoaded("genesis");

        if (isGenesLoaded) {
            return new GenesisCompat();
        } else {
            return new Compat() {};
        }
    }
}
