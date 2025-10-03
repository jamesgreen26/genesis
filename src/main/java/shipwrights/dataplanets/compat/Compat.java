package shipwrights.dataplanets.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
import shipwrights.dataplanets.DPPackets;
import shipwrights.dataplanets.MutableTags;
import shipwrights.dataplanets.space.S2PSyncPacket;
import shipwrights.dataplanets.space.StarSystemCreator;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;
import shipwrights.genesis.GenesisMod;

public class Compat {

    public static  String[] SURFACE_BLOCKS;
    public static String COMPAT_MOD = "genesis";
    public static ResourceKey<DimensionType> SPACE_DIMENSION_TYPE;
    public static ResourceKey<Biome> SPACE_BIOME;

    /**
     * This method is called when Dataplanets is initialized
     * @param bus
     */
    public static void modEventBusLoad(IEventBus bus)
    {

    }

    /**
     * this method is called after a planet's level stem has been re/created
     * @param planetData
     */
    public static void postLoadPlanet(CompoundTag planetData)
    {

        float[] color = getPlanetColor(planetData);

        GenesisMod.registerPlanet(ResourceLocation.fromNamespaceAndPath("dataplanets", planetData.getString("name")), planetData.getInt("scaleClient") / 10d, planetData.getInt("radiusClient") / 3d, color[0], color[1], color[2]);
    }

    private static float[] getPlanetColor(CompoundTag planetData) {
        int temperature = planetData.getInt("temperature");
        String planetType = planetData.getString("planetType");
        String seaBlock = planetData.getString("seaBlock");
        String generalBlock = planetData.getString("generalBlock");

        float r = 0.5f, g = 0.5f, b = 0.5f;

        // Color based on planet type
        if ("gaseous".equals(planetType)) {
            // Gas giants - pale blues/purples
            r = 0.7f;
            g = 0.75f;
            b = 0.85f;
        } else if ("ocean".equals(planetType)) {
            // Ocean worlds - blue
            r = 0.2f;
            g = 0.4f;
            b = 0.8f;
        } else if ("icy".equals(planetType)) {
            // Ice worlds - white/cyan
            r = 0.85f;
            g = 0.9f;
            b = 0.95f;
        } else {
            // Rocky planets - color based on surface block and temperature
            if (generalBlock.contains("magma")) {
                // Hot volcanic - red/orange
                r = 0.9f;
                g = 0.3f;
                b = 0.1f;
            } else if (generalBlock.contains("netherrack")) {
                r = 0.7f;
                g = 0.3f;
                b = 0.3f;
            } else if (generalBlock.contains("end_stone")) {
                r = 0.9f;
                g = 0.9f;
                b = 0.7f;
            } else if (generalBlock.contains("sandstone")) {
                r = 0.85f;
                g = 0.7f;
                b = 0.5f;
            } else if (generalBlock.contains("basalt") || generalBlock.contains("blackstone")) {
                r = 0.25f;
                g = 0.25f;
                b = 0.3f;
            } else {
                // Default rocky - gray/brown
                r = 0.5f;
                g = 0.45f;
                b = 0.4f;
            }

            // Adjust for temperature
            if (temperature > 600) {
                r = Math.min(1.0f, r + 0.3f);
                g = Math.max(0.0f, g - 0.1f);
                b = Math.max(0.0f, b - 0.2f);
            } else if (temperature < 273) {
                r = Math.min(1.0f, r + 0.2f);
                g = Math.min(1.0f, g + 0.2f);
                b = Math.min(1.0f, b + 0.3f);
            }
        }
        float[] rgb = new float[3];
        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;
        return rgb;
    }

    public static void loadCompat(String compatmod)
    {
        SURFACE_BLOCKS = new String[]{"minecraft:stone","minecraft:cobblestone","minecraft:end_stone","minecraft:netherrack","minecraft:blackstone","minecraft:sandstone","minecraft:red_sandstone","minecraft:basalt"};
        SPACE_BIOME = ResourceKey.create(Registries.BIOME,GenesisMod.SPACE_DIM);
        SPACE_DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,GenesisMod.SPACE_DIM);
    }

    /**
     * this method is called when building an orbit of a planet
     * @param biomeHolder
     * @return
     */
    public static ChunkGenerator spaceGenerator(Holder.Reference<Biome> biomeHolder)
    {

        return new NoiseBasedChunkGenerator(new FixedBiomeSource(biomeHolder),Holder.direct(NoiseGeneratorSettings.dummy()));
    }

    /**
     * this method is called immediately after first discovering a system, when all chunks are saved, and whenever a player logs in
     */
    public static void postLoadWorld()
    {
        DPPackets.INSTANCE.send(PacketDistributor.ALL.noArg(),new S2PSyncPacket(StarSystemCreator.getDynamicDataOrNew()));

    }
}
