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
        GenesisMod.registerPlanet(ResourceLocation.fromNamespaceAndPath("dataplanets",planetData.getString("name")),planetData.getInt("scaleClient") / 10d,planetData.getInt("radiusClient"),1,1,1);
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
