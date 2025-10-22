package shipwrights.dataplanets;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.dataplanets.naming.FantasySystemNameGenerator;
import shipwrights.dataplanets.util.RegistryUtil;

@Mod.EventBusSubscriber
public class DataplanetsMod {
    public static final String MOD_ID = "dataplanets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final FantasySystemNameGenerator FANTASY_SYSTEM_NAME_GENERATOR = new FantasySystemNameGenerator();

    @SubscribeEvent
    public static void onDataReload(AddReloadListenerEvent event) {
        event.addListener(FANTASY_SYSTEM_NAME_GENERATOR);
        LOGGER.info("Registered Fantasy System Name Generator reload listener");
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();

        createPlanet(server);
    }

    static void createPlanet(MinecraftServer server) {
        ResourceKey<NoiseGeneratorSettings> noiseSettingsKey =
                ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.withDefaultNamespace("overworld"));

        Holder<NoiseGeneratorSettings> noiseSettings =
                server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(noiseSettingsKey);


        Holder<WorldPreset> overworldPreset =
                server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.NORMAL);

        WorldPreset preset = overworldPreset.value();
        if (preset.overworld().isPresent()) {
            BiomeSource biomeSource = preset.overworld().get().generator().getBiomeSource();



            LevelStem stem = new LevelStem(
                    server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE)
                            .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.withDefaultNamespace("overworld"))),
                    new NoiseBasedChunkGenerator(biomeSource, noiseSettings)
            );
            RegistryUtil.registerLevelStem(server, ResourceLocation.fromNamespaceAndPath(MOD_ID, "new_level"), stem);

            LOGGER.info("registered dimension");
        } else {
            LOGGER.error("did not work");
        }
    }
}
