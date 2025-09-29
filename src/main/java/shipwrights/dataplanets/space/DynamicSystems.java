package shipwrights.dataplanets.space;

import shipwrights.dataplanets.compat.Compat;
import shipwrights.dataplanets.interfaces.IUnfreezableRegistry;
import shipwrights.dataplanets.registry.DPBlocks;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DeltaFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.level.LevelEvent;
import shipwrights.genesis.GenesisMod;

import java.util.*;

/**
 * Big props to <a href="https://github.com/iPortalTeam/DimLib/blob/1.21/src/main/java/qouteall/dimlib/DynamicDimensionsImpl.java">...</a>
 * This class can dynamically register most aspects of a world, it should remain generic use Compat.class
 */
public class DynamicSystems {
    public static Registry<Biome> BIOMES = null;
    public static Registry<ConfiguredWorldCarver<?>> CONFIGURED_CARVERS = null;
    public static Registry<PlacedFeature> PLACED_FEATURES = null;
    public static Registry<DimensionType> DIMENSION_TYPE = null;
    public static Registry<ConfiguredFeature<?,?>> CONFIGURED_FEATURES = null;
    public static Registry<LevelStem> LEVEL_STEMS = null;
    public static Registry<NormalNoise.NoiseParameters> NOISE = null;
    public static Map<String,String> TRANSLATIONS = new HashMap<>();
    public static Map<String,float[]> RAIN_COLOUR = new HashMap<>();
    public static int frozeTimes = 0;

    /**
     * called to load "datapack" resources into a world
     * used every time apart from the first when you should use onGenSetup instead.
     */
    public static void loadDynamicResources()
    {
        //TODO: 20 is a somewhat arbitrary number, check this works on a big modpack
        //I would have thought this would have more problems on a smaller modpack actually...
        if(DynamicSystems.allRegistriesFrozen() && frozeTimes>20)
        {

            CompoundTag tag = StarSystemCreator.getDynamicDataOrNew();
            for(String system: tag.getAllKeys())
            {
                if(tag.getTagType(system)== Tag.TAG_COMPOUND)
                {
                    CompoundTag specificSystem = tag.getCompound(system);
                    DynamicSystems.makeStar(specificSystem);

                    for(String planet: specificSystem.getAllKeys())
                    {
                        if(specificSystem.getTagType(planet)== Tag.TAG_COMPOUND)
                        {
                            CompoundTag specificPlanet = specificSystem.getCompound(planet);
                            //DynamicSystems.makeOrbit(specificPlanet);
                            DynamicSystems.makePlanet(specificPlanet);

                        }
                    }

                }
            }

        }
    }

    public static boolean allRegistriesFrozen()
    {
        return BIOMES!=null
                && CONFIGURED_CARVERS !=null
                && PLACED_FEATURES!=null
                && DIMENSION_TYPE!=null
                && CONFIGURED_FEATURES!=null
                && LEVEL_STEMS!=null
                && NOISE!=null;
    }

    public static ResourceKey<Biome> makeBiome(CompoundTag biomeData)
    {
        ResourceKey<Biome> biomeKey = ResourceKey.create(BIOMES.key(), ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_terrain"));

        if(!BIOMES.containsKey(biomeKey))
        {
            Biome biome = new Biome.BiomeBuilder()
                    .downfall(biomeData.getFloat("downfall"))
                    .temperature(biomeData.getFloat("climate"))
                    .hasPrecipitation(biomeData.getBoolean("hasRain"))
                    .specialEffects(new BiomeSpecialEffects.Builder()
                            .skyColor(biomeData.getInt("skyColour"))
                            .fogColor(biomeData.getInt("fogColour"))
                            .waterColor(biomeData.getInt("waterColour"))
                            .waterFogColor(biomeData.getInt("waterFogColour"))
                            .grassColorOverride(biomeData.getInt("grassColour"))
                            .foliageColorOverride(biomeData.getInt("foliageColour")).build())
                    .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                    .generationSettings(builder(new BiomeGenerationSettingsBuilder(BiomeGenerationSettings.EMPTY),biomeData).build()).build();


            ((IUnfreezableRegistry) BIOMES).setRegFrozen(false);
            ((MappedRegistry<Biome>) BIOMES).register(
                    biomeKey,
                    biome,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) BIOMES).setRegFrozen(true);
        }
        return biomeKey;
    }

    private static BiomeGenerationSettings.PlainBuilder builder(BiomeGenerationSettingsBuilder builder,CompoundTag biomeData)
    {
        Holder.Reference<ConfiguredWorldCarver<?>> canyon = CONFIGURED_CARVERS.getHolderOrThrow(Carvers.CANYON);
        Holder.Reference<ConfiguredWorldCarver<?>> cave = CONFIGURED_CARVERS.getHolderOrThrow(Carvers.CAVE);
        Holder.Reference<ConfiguredWorldCarver<?>> cave_extra = CONFIGURED_CARVERS.getHolderOrThrow(Carvers.CAVE_EXTRA_UNDERGROUND);


        Holder.Reference<PlacedFeature> dripstone = PLACED_FEATURES.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.tryParse("large_dripstone"))).get();
        Holder.Reference<PlacedFeature> dripstone_cluster = PLACED_FEATURES.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("dripstone_cluster"))).get();
        Holder.Reference<PlacedFeature> pointed_dripstone = PLACED_FEATURES.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("pointed_dripstone"))).get();

        builder.addCarver(GenerationStep.Carving.AIR,canyon)
                .addCarver(GenerationStep.Carving.AIR,cave)
                .addCarver(GenerationStep.Carving.AIR,cave_extra);

        byte[] flavour = biomeData.getByteArray("flavour");
        if(flavour[0]==1)
        {
            builder.addFeature(0,dripstone_cluster);
        }
        if(flavour[1]==1)
        {
            builder.addFeature(0,dripstone);
        }
        if(flavour[2]==1)
        {
            builder.addFeature(0,pointed_dripstone);
        }
        if(biomeData.contains("treeTrunk"))
        {
            List<ResourceKey<PlacedFeature>> features = makeTreeLike(biomeData);
            for(ResourceKey<PlacedFeature> feature: features)
            {
                builder.addFeature(0,PLACED_FEATURES.getHolder(feature).get());
            }
        }


        List<ResourceKey<PlacedFeature>> features = makeOres(biomeData);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,PLACED_FEATURES.getHolder(feature).get());
        }
        features = makeLakes(biomeData);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,PLACED_FEATURES.getHolder(feature).get());
        }
        features = makeRocks(biomeData);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,PLACED_FEATURES.getHolder(feature).get());
        }


         features = makeDelta(biomeData);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,PLACED_FEATURES.getHolder(feature).get());
        }



        return builder;
    }

    public static ResourceKey<DimensionType> makeDimType(CompoundTag planetData)
    {
        ResourceKey<DimensionType> dimKey = ResourceKey.create(DIMENSION_TYPE.key(),ResourceLocation.tryBuild("dataplanets",planetData.getString("name")));

        if(!DIMENSION_TYPE.containsKey(dimKey))
        {
            ResourceLocation effects;
            if(planetData.getBoolean("hasAtmosphere"))
            {
                if(planetData.getBoolean("hasOxygen"))
                {
                    effects=ResourceLocation.fromNamespaceAndPath("minecraft","overworld");
                }
                else
                {
                    effects=ResourceLocation.fromNamespaceAndPath("minecraft","the_nether");
                }

            }
            else
            {
                effects=ResourceLocation.fromNamespaceAndPath("minecraft","the_end");
            }

            DimensionType dimensionType = new DimensionType(
                    OptionalLong.empty(),
                    planetData.getInt("solarPower")>3,
                    false,
                    planetData.getInt("temperature")>600,
                    true,
                    1,
                    true,
                    false,
                    -64,
                    384,
                    284,
                    BlockTags.INFINIBURN_OVERWORLD,
                    effects,
                    0,
                    new DimensionType.MonsterSettings(planetData.getInt("temperature")>400,false, UniformInt.of(0,7),0)
            );



            ((IUnfreezableRegistry) DIMENSION_TYPE).setRegFrozen(false);
            ((MappedRegistry<DimensionType>) DIMENSION_TYPE).register(
                    dimKey,
                    dimensionType,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) DIMENSION_TYPE).setRegFrozen(true);
        }

        return dimKey;
    }
    public static List<ResourceKey<PlacedFeature>> makeTreeLike(CompoundTag biomeData)
    {
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(CONFIGURED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_tree"));
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(PLACED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_tree"));
        if(!PLACED_FEATURES.containsKey(placedKey))
        {

            BlockState trunkState = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(biomeData.getString("treeTrunk"))).defaultBlockState();
            BlockState leafState = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(biomeData.getString("treeLeaves"))).defaultBlockState();

            //TODO: Could probably add other tree variants
            TreeConfiguration configuration;
            if(biomeData.getByteArray("flavour")[4]==1)
            {
                configuration = new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(trunkState),
                        new StraightTrunkPlacer(4,2,0),
                        BlockStateProvider.simple(leafState),
                        new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.ZERO, 3),
                        Optional.empty(),
                        new TwoLayersFeatureSize(1,0,1)).build();
            }
            else
            {
                configuration = new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(trunkState),
                        new FancyTrunkPlacer(5,3,0),
                        BlockStateProvider.simple(leafState),
                        new BlobFoliagePlacer(ConstantInt.of(3), ConstantInt.ZERO, 3),
                        Optional.empty(),
                        new TwoLayersFeatureSize(1,0,1)).build();
            }

            ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.TREE,configuration);



            ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(false);
            ((MappedRegistry<ConfiguredFeature<?,?>>) CONFIGURED_FEATURES).register(
                    configuredKey,
                    feature,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(true);

            List<PlacementModifier> modifiers = new ArrayList<>();
            SimpleWeightedRandomList<IntProvider> weight = SimpleWeightedRandomList.<IntProvider>builder()
                    .add(ConstantInt.of(0),19)
                    .add(ConstantInt.of(1),1).build();
            modifiers.add(CountPlacement.of(new WeightedListInt(weight)));
            modifiers.add(InSquarePlacement.spread());
            modifiers.add(SurfaceWaterDepthFilter.forMaxDepth(0));
            modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR));

            PlacedFeature placedFeature = new PlacedFeature(CONFIGURED_FEATURES.getHolderOrThrow(configuredKey),modifiers);



            ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(false);
            ((MappedRegistry<PlacedFeature>) PLACED_FEATURES).register(
                    placedKey,
                    placedFeature,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(true);
            features.add(placedKey);
        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeDelta(CompoundTag biomeData)
    {
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.getByteArray("flavour")[3]==1 && biomeData.getInt("temperature")>500)
        {
            ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(CONFIGURED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_delta"));
            ResourceKey<PlacedFeature> placedKey = ResourceKey.create(PLACED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_delta"));
            if(!PLACED_FEATURES.containsKey(placedKey))
            {
                DeltaFeatureConfiguration configuration = new DeltaFeatureConfiguration(Blocks.LAVA.defaultBlockState(),Blocks.MAGMA_BLOCK.defaultBlockState(), UniformInt.of(3,7),UniformInt.of(0,2));
                ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.DELTA_FEATURE,configuration);



                ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(false);
                ((MappedRegistry<ConfiguredFeature<?,?>>) CONFIGURED_FEATURES).register(
                        configuredKey,
                        feature,
                        Lifecycle.stable() // use built-in registration info for now
                );
                ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(true);

                List<PlacementModifier> modifiers = new ArrayList<>();
                modifiers.add(CountOnEveryLayerPlacement.of(40));
                PlacedFeature placedFeature = new PlacedFeature(CONFIGURED_FEATURES.getHolderOrThrow(configuredKey),modifiers);



                ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(false);
                ((MappedRegistry<PlacedFeature>) PLACED_FEATURES).register(
                        placedKey,
                        placedFeature,
                        Lifecycle.stable() // use built-in registration info for now
                );
                ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(true);
                features.add(placedKey);
            }

        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeOres(CompoundTag biomeData)
    {
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("biome_ores"))
        {
            ListTag ores = (ListTag) biomeData.get("biome_ores");
            for (int i = 0; i < ores.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(CONFIGURED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_ore_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(PLACED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_ore_"+i));
                if(!PLACED_FEATURES.containsKey(placedKey))
                {
                    BlockState oreState = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(ores.getString(i))).defaultBlockState();
                    OreConfiguration configuration = new OreConfiguration(List.of(OreConfiguration.target(new TagMatchTest(BlockTags.DIRT),oreState)),10);
                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(OreFeature.ORE,configuration);



                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) CONFIGURED_FEATURES).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(CountPlacement.of(16));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-11),VerticalAnchor.belowTop(112)));
                    PlacedFeature placedFeature = new PlacedFeature(CONFIGURED_FEATURES.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) PLACED_FEATURES).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(true);
                    features.add(placedKey);
                }



            }
        }
        return features;
    }
    public static List<ResourceKey<PlacedFeature>> makeLakes(CompoundTag biomeData)
    {
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("lakeFluids"))
        {
            ListTag lakes = (ListTag) biomeData.get("lakeFluids");
            BlockState barrier = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(biomeData.getString("generalBlock"))).defaultBlockState();
            for (int i = 0; i < lakes.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(CONFIGURED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_lake_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(PLACED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_lake_"+i));
                if(!PLACED_FEATURES.containsKey(placedKey))
                {
                    BlockState fluid = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(lakes.getString(i))).defaultBlockState();

                    LakeFeature.Configuration lakeConfig = new LakeFeature.Configuration(BlockStateProvider.simple(fluid),BlockStateProvider.simple(barrier));

                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.LAKE,lakeConfig);



                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) CONFIGURED_FEATURES).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(RarityFilter.onAverageOnceEvery(200));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG));
                    PlacedFeature placedFeature = new PlacedFeature(CONFIGURED_FEATURES.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) PLACED_FEATURES).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(true);
                    features.add(placedKey);
                }



            }
        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeRocks(CompoundTag biomeData)
    {
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("rock_blocks"))
        {
            ListTag rocks = (ListTag) biomeData.get("rock_blocks");
            for (int i = 0; i < rocks.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(CONFIGURED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_rock_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(PLACED_FEATURES.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_rock_"+i));
                if(!PLACED_FEATURES.containsKey(placedKey))
                {
                    BlockState rock = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(rocks.getString(i))).defaultBlockState();

                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.FOREST_ROCK,new BlockStateConfiguration(rock));


                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) CONFIGURED_FEATURES).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) CONFIGURED_FEATURES).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(RarityFilter.onAverageOnceEvery(2));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING));
                    PlacedFeature placedFeature = new PlacedFeature(CONFIGURED_FEATURES.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) PLACED_FEATURES).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) PLACED_FEATURES).setRegFrozen(true);
                    features.add(placedKey);
                }



            }
        }
        return features;
    }

    private static SurfaceRules.RuleSource planetarySurfaceRuleSource(boolean hasLife)
    {
        ImmutableList.Builder<SurfaceRules.RuleSource> builder = ImmutableList.builder();
        builder.add(SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())));
        if(hasLife)
        {
            //builder.add(SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0),SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())));
            builder.add(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())));
        }

        return SurfaceRules.sequence(builder.build().toArray(SurfaceRules.RuleSource[]::new));
    }

    //this does technically mean we are limited to planets with 1 character designations
    //we don't actually have to use the latin alphabet though, I think.
    public static String planetStarName(CompoundTag planetData)
    {
        String v = planetData.getString("name");
        return v.substring(0,v.length()-1);
    }


    public static LevelStem makeStar(CompoundTag systemData)
    {
        ResourceKey<LevelStem> starKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets", systemData.getString("systemName")));
        if(!LEVEL_STEMS.containsKey(starKey))
        {
            Holder.Reference<Biome> biomeHolder = BIOMES.getHolderOrThrow(Compat.SPACE_BIOME);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(),biomeHolder,List.of())
                    .withBiomeAndLayers(List.of(new FlatLayerInfo(1,Blocks.BEDROCK),new FlatLayerInfo(50,Blocks.LAVA),new FlatLayerInfo(50, DPBlocks.DENSE_GAS.get())),Optional.empty(),biomeHolder);
            Holder.Reference<DimensionType> holder = DIMENSION_TYPE.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);

            FlatLevelSource flatLevelSource = new FlatLevelSource(settings);
            LevelStem stem = new LevelStem(holder,flatLevelSource);

            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) LEVEL_STEMS).register(
                    starKey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );
        }
        return LEVEL_STEMS.get(starKey);
    }

    public static LevelStem makeGasPlanet(CompoundTag planetData)
    {
        ResourceKey<LevelStem> starKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets", planetData.getString("name")));
        if(!LEVEL_STEMS.containsKey(starKey))
        {
            Holder.Reference<Biome> biomeHolder = BIOMES.getHolderOrThrow(Compat.SPACE_BIOME);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(),biomeHolder,List.of())
                    .withBiomeAndLayers(List.of(new FlatLayerInfo(1,Blocks.BEDROCK),new FlatLayerInfo(50,Blocks.LAVA),new FlatLayerInfo(50, DPBlocks.DENSE_GAS.get())),Optional.empty(),biomeHolder);
            Holder.Reference<DimensionType> holder = DIMENSION_TYPE.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);

            FlatLevelSource flatLevelSource = new FlatLevelSource(settings);
            LevelStem stem = new LevelStem(holder,flatLevelSource);

            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) LEVEL_STEMS).register(
                    starKey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );
        }
        Compat.postLoadPlanet(planetData);
        return LEVEL_STEMS.get(starKey);
    }

    public static LevelStem makePlanet(CompoundTag planetData)
    {
        ResourceKey<LevelStem> resourcekey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",planetData.getString("name")));
        Holder.Reference<DimensionType> holder = DIMENSION_TYPE.getHolderOrThrow(makeDimType(planetData));
        if(!LEVEL_STEMS.containsKey(resourcekey))
        {

            Holder<NormalNoise.NoiseParameters> OFFSET = NOISE.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","offset")));

            NoiseGeneratorSettings settings = new NoiseGeneratorSettings(
                    NoiseSettings.create(-64, 384, 2, 2),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(planetData.getString("generalBlock"))).defaultBlockState(),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(planetData.getString("seaBlock"))).defaultBlockState(),
                    new NoiseRouter(
                            DensityFunctions.constant(0),
                            DensityFunctions.constant(0),
                            DensityFunctions.constant(0),
                            DensityFunctions.constant(0),
                            DensityFunctions.shiftedNoise2d(
                                    DensityFunctions.shiftA(OFFSET),
                                    DensityFunctions.shiftB(OFFSET),
                                    0.25F,
                                    NOISE.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","temperature")))
                                    ),
                            DensityFunctions.shiftedNoise2d(
                                    DensityFunctions.shiftA(OFFSET),
                                    DensityFunctions.shiftB(OFFSET),
                                    0.25F,
                                    NOISE.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","vegetation")))
                            ),
                            DensityFunctions.constant(0),
                            DensityFunctions.shiftedNoise2d(
                                    DensityFunctions.shiftA(OFFSET),
                                    DensityFunctions.shiftB(OFFSET),
                                    0.25F,
                                    NOISE.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","erosion")))
                            ),
                            DensityFunctions.constant(0),
                            DensityFunctions.shiftedNoise2d(
                                    DensityFunctions.shiftA(OFFSET),
                                    DensityFunctions.shiftB(OFFSET),
                                    0.25F,
                                    NOISE.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","ridge")))
                            ),
                            DensityFunctions.constant(0),
                            DensityFunctions.add(
                                    DensityFunctions.yClampedGradient(-64,320,1,-1),
                                    DensityFunctions.noise(NOISE.getHolderOrThrow(Noises.GRAVEL),planetData.getFloat("nr1"),planetData.getFloat("nr2"))),
                            DensityFunctions.constant(0),
                            DensityFunctions.constant(0),
                            DensityFunctions.constant(0)),
                    planetarySurfaceRuleSource(planetData.getBoolean("hasOxygen")&&planetData.getBoolean("hasAtmosphere")),
                    new OverworldBiomeBuilder().spawnTarget(),
                    planetData.getInt("seaLevel"),
                    false,
                    true,
                    true,
                    false
            );
            //Holder.Reference<Biome> biomeHolder = BIOMES.getHolderOrThrow(makeBiome(planetData));

            List<Pair<Climate.ParameterPoint,Holder<Biome>>> biomes = new ArrayList<>();
            ListTag biomesTag = planetData.getList("biomes",ListTag.TAG_COMPOUND);
            for (int i = 0; i < biomesTag.size(); i++) {
                CompoundTag b = biomesTag.getCompound(i);
                biomes.add(new Pair<>(
                        new Climate.ParameterPoint(
                                Climate.Parameter.point(b.getFloat("temp")),
                                Climate.Parameter.point(b.getFloat("humid")),
                                Climate.Parameter.point(1),
                                Climate.Parameter.point(b.getFloat("erode")),
                                Climate.Parameter.point(1),
                                Climate.Parameter.point(b.getFloat("wierd")),
                                0
                        ),BIOMES.getHolderOrThrow(makeBiome(b))));
            }

            MultiNoiseBiomeSource n = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomes));
            NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(n, Holder.direct(settings));
            LevelStem stem = new LevelStem(holder,noiseBasedChunkGenerator);

            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) LEVEL_STEMS).register(
                    resourcekey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );

            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(true);

        }

        Compat.postLoadPlanet(planetData);
        return LEVEL_STEMS.get(resourcekey);
    }

    public static LevelStem makeOrbit(CompoundTag planetData)
    {
        ResourceKey<LevelStem> orbitKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",planetData.getString("name")+"_orbit"));

        if(!LEVEL_STEMS.containsKey(orbitKey))
        {
            Holder.Reference<DimensionType> orbitHolder = DIMENSION_TYPE.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);
            Holder.Reference<Biome> orbitBiomeHolder = BIOMES.getHolderOrThrow(Compat.SPACE_BIOME);

            LevelStem orbit = new LevelStem(orbitHolder,Compat.spaceGenerator(orbitBiomeHolder));

            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) LEVEL_STEMS).register(
                    orbitKey,
                    orbit,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) LEVEL_STEMS).setRegFrozen(true);
            return orbit;
        }
        return LEVEL_STEMS.get(orbitKey);


    }

    public static void makeDynamicWorld(MinecraftServer server, String name, LevelStem stem)
    {
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryBuild("dataplanets",name));

        if(!server.levels.containsKey(dimensionKey))
        {
            ChunkProgressListener listener = server.progressListenerFactory.create(server.getWorldData().getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS));

            ServerLevelData serverleveldata = server.getWorldData().overworldData();

            DerivedLevelData derivedleveldata = new DerivedLevelData(server.getWorldData(), serverleveldata);
            ServerLevel serverlevel1 = new ServerLevel(server, Util.backgroundExecutor(), server.storageSource, derivedleveldata, dimensionKey, stem, listener, server.getWorldData().isDebugWorld(), BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()), ImmutableList.of(), false, server.overworld().getRandomSequences());
            server.overworld().getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(serverlevel1.getWorldBorder()));

            server.levels.put(dimensionKey, serverlevel1);
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(server.levels.get(dimensionKey)));
            Compat.postLoadWorld();


        }
    }

    /**
     * Called to generate the level/world save data to save to disk
     * goes through all planets in the data file and creates their save data
     * @param server
     */
    public static void onGenSetup(MinecraftServer server)
    {
        CompoundTag systems = StarSystemCreator.getDynamicDataOrNew();
        for(String systemId: systems.getAllKeys())
        {
            if(systems.getTagType(systemId)== Tag.TAG_COMPOUND)
            {
                CompoundTag systemData = systems.getCompound(systemId);
                makeDynamicWorld(server,systemId,makeStar(systemData));

                for(String planetId: systemData.getAllKeys())
                {
                    CompoundTag planetData = systemData.getCompound(planetId);
                    if(systemData.getTagType(planetId)== Tag.TAG_COMPOUND)
                    {
                        System.out.println("creating planet: "+planetId);
                        if(planetData.contains("planetType") && planetData.getString("planetType").equals("gaseous"))
                        {
                            makeDynamicWorld(server,planetId,makeGasPlanet(planetData));
                        }
                        else
                        {
                            makeDynamicWorld(server,planetId, makePlanet(planetData));
                        }

                        //makeDynamicWorld(server,planetId+"_orbit",makeOrbit(planetData));
                    }
                    //TODO: move next bit to a compat class at some point
                    GenesisMod.registerPlanet(ResourceLocation.fromNamespaceAndPath("dataplanets",planetId),planetData.getInt("scaleClient"),planetData.getInt("radiusClient"),1,1,1);
                }
            }
        }
    }


}
