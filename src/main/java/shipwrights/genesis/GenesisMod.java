package shipwrights.genesis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.space.Celestial;

import java.util.Optional;

@Mod(GenesisMod.MOD_ID)
public class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final ResourceLocation SPACE = id("great_unknown");

    public static ResourceLocation id(String location) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, location);
    }

    public static boolean isSpaceDimension(Level level) {
        return level.dimension().location().equals(SPACE);
    }

    public static Optional<Celestial> celestial(Level level) {
        var registry = level.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        return registry.getOptional(level.dimension().location());
    }
}
