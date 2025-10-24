package shipwrights.dataplanets.systemCreation.dimension.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.systemCreation.PlanetData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;

public class BiomeTags {
    public static final TagKey<Biome> HAS_WEATHER_STATION = create("has_weather_station");

    private static TagKey<Biome> create(String string) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, string));
    }

    public static void addTagsToBiome(SystemCreator.SystemCreationContext context, ResourceLocation biomeLocation, PlanetData planetData, double variationFactor) {

        if (context.random.nextDouble() > 0.5 && planetData.atmosphericDensity() > 0.3) {
            RegistryUtil.addBiomeToTag(context.server, biomeLocation, HAS_WEATHER_STATION);
        }
    }
}
