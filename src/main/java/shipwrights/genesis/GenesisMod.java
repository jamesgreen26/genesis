package shipwrights.genesis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.content.GenesisBlocks;
import shipwrights.genesis.content.GenesisCreativeTabs;
import shipwrights.genesis.content.GenesisEntityTypes;
import shipwrights.genesis.content.GenesisItems;
import shipwrights.genesis.content.GenesisMobEffects;
import shipwrights.genesis.network.GenesisNetwork;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.worldgen.GenesisDensityFunctions;
import shipwrights.genesis.worldgen.GenesisFeatures;

import java.util.Optional;

@Mod(GenesisMod.MOD_ID)
public class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final ResourceLocation SPACE = id("great_unknown");

    public GenesisMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        GenesisBlocks.BLOCKS.register(modBus);
        GenesisEntityTypes.ENTITY_TYPES.register(modBus);
        GenesisMobEffects.MOB_EFFECTS.register(modBus);
        GenesisItems.ITEMS.register(modBus);
        GenesisCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        GenesisDensityFunctions.DENSITY_FUNCTION_TYPES.register(modBus);
        GenesisFeatures.FEATURES.register(modBus);
        modBus.addListener(GenesisOverridesPack::addPackFinders);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GenesisCommonConfig.SPEC);
        GenesisNetwork.register();
    }

    public static ResourceLocation id(String location) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, location);
    }

    /// Returns true when the dimension is the main space dimension
    public static boolean isSpaceDimension(Level level) {
        return level.dimension().location().equals(SPACE);
    }

    /// Returns true when the dimension should behave like space, with no void and no gravity
    public static boolean isSpaceLikeDimension(Level level) {
        return level.dimension().location().equals(SPACE);
    }

    public static Optional<Celestial> celestial(Level level) {
        var registry = level.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        return registry.getOptional(level.dimension().location());
    }
}
