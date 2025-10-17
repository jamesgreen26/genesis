package shipwrights.dataplanets.space;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.server.ServerLifecycleHooks;
import shipwrights.dataplanets.compat.Compat;
import shipwrights.dataplanets.interfaces.IUnfreezableRegistry;
import shipwrights.dataplanets.registry.DPBlocks;
import shipwrights.dataplanets.util.TaskUtil;

import java.util.*;

public class DynamicSystems {

    public static Map<String,String> TRANSLATIONS = new HashMap<>();
    public static Map<String,float[]> RAIN_COLOUR = new HashMap<>();

    public static void loadDynamicResources(RegistryAccess.Frozen registryAccess, DimensionDataStorage storage){
        CompoundTag tag = StarSystemCreator.getDynamicDataOrNew(storage);

        if (tag.isEmpty()){
            Pair<CompoundTag, String> data = StarSystemCreator.makeSystem(storage,8,12);
            generateNewSystem(data.getFirst(),data.getSecond(), registryAccess, storage);
        }

        tag = StarSystemCreator.getDynamicDataOrNew(storage);

        for(String system: tag.getAllKeys())
        {
            if(tag.getTagType(system) == Tag.TAG_COMPOUND)
            {
                CompoundTag specificSystem = tag.getCompound(system);
                DynamicSystems.makeDynamicWorld(system,DynamicSystems.makeStar(specificSystem, registryAccess), storage);

                for(String planet: specificSystem.getAllKeys())
                {
                    if(specificSystem.getTagType(planet) == Tag.TAG_COMPOUND)
                    {
                        CompoundTag specificPlanet = specificSystem.getCompound(planet);
                        //DynamicSystems.makeOrbit(specificPlanet);
                        DynamicSystems.makeDynamicWorld(planet,DynamicSystems.makePlanet(specificPlanet, registryAccess), storage);

                    }
                }

            }
        }
    }

    public static ResourceKey<Biome> makeBiome(CompoundTag biomeData, RegistryAccess.Frozen access)  {
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        ResourceKey<Biome> biomeKey = ResourceKey.create(biomeRegistry.key(), ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_terrain"));

        if(!biomeRegistry.containsKey(biomeKey))
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
                    .generationSettings(builder(new BiomeGenerationSettingsBuilder(BiomeGenerationSettings.EMPTY),biomeData, access).build()).build();


            ((IUnfreezableRegistry) biomeRegistry).setRegFrozen(false);
            ((MappedRegistry<Biome>) biomeRegistry).register(
                    biomeKey,
                    biome,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) biomeRegistry).setRegFrozen(true);
        }
        return biomeKey;
    }

    private static BiomeGenerationSettings.PlainBuilder builder(BiomeGenerationSettingsBuilder builder,CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        Registry<ConfiguredWorldCarver<?>> configuredCarversRegistry = access.registryOrThrow(Registries.CONFIGURED_CARVER);
        Holder.Reference<ConfiguredWorldCarver<?>> canyon = configuredCarversRegistry.getHolderOrThrow(Carvers.CANYON);
        Holder.Reference<ConfiguredWorldCarver<?>> cave = configuredCarversRegistry.getHolderOrThrow(Carvers.CAVE);
        Holder.Reference<ConfiguredWorldCarver<?>> cave_extra = configuredCarversRegistry.getHolderOrThrow(Carvers.CAVE_EXTRA_UNDERGROUND);


        Holder.Reference<PlacedFeature> dripstone = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.tryParse("large_dripstone"))).get();
        Holder.Reference<PlacedFeature> dripstone_cluster = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("dripstone_cluster"))).get();
        Holder.Reference<PlacedFeature> pointed_dripstone = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("pointed_dripstone"))).get();

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
            List<ResourceKey<PlacedFeature>> features = makeTreeLike(biomeData, access);
            for(ResourceKey<PlacedFeature> feature: features)
            {
                builder.addFeature(0,placedFeatureRegistry.getHolder(feature).get());
            }
        }


        List<ResourceKey<PlacedFeature>> features = makeOres(biomeData, access);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,placedFeatureRegistry.getHolder(feature).get());
        }
        features = makeLakes(biomeData, access);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,placedFeatureRegistry.getHolder(feature).get());
        }
        features = makeRocks(biomeData, access);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,placedFeatureRegistry.getHolder(feature).get());
        }


        features = makeDelta(biomeData, access);
        for(ResourceKey<PlacedFeature> feature: features)
        {
            builder.addFeature(0,placedFeatureRegistry.getHolder(feature).get());
        }



        return builder;
    }

    public static ResourceKey<DimensionType> makeDimType(CompoundTag planetData, RegistryAccess.Frozen access)
    {   Registry<DimensionType> dimensionRegistry = access.registryOrThrow(Registries.DIMENSION_TYPE);
        ResourceKey<DimensionType> dimKey = ResourceKey.create(dimensionRegistry.key(),ResourceLocation.tryBuild("dataplanets",planetData.getString("name")));

        if(!dimensionRegistry.containsKey(dimKey))
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



            ((IUnfreezableRegistry) dimensionRegistry).setRegFrozen(false);
            ((MappedRegistry<DimensionType>) dimensionRegistry).register(
                    dimKey,
                    dimensionType,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) dimensionRegistry).setRegFrozen(true);
        }

        return dimKey;
    }
    public static List<ResourceKey<PlacedFeature>> makeTreeLike(CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<ConfiguredFeature<?,?>> configuredFeaturesRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeaturesRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_tree"));
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_tree"));
        if(!placedFeatureRegistry.containsKey(placedKey))
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



            ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(false);
            ((MappedRegistry<ConfiguredFeature<?,?>>) configuredFeaturesRegistry).register(
                    configuredKey,
                    feature,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(true);

            List<PlacementModifier> modifiers = new ArrayList<>();
            SimpleWeightedRandomList<IntProvider> weight = SimpleWeightedRandomList.<IntProvider>builder()
                    .add(ConstantInt.of(0),19)
                    .add(ConstantInt.of(1),1).build();
            modifiers.add(CountPlacement.of(new WeightedListInt(weight)));
            modifiers.add(InSquarePlacement.spread());
            modifiers.add(SurfaceWaterDepthFilter.forMaxDepth(0));
            modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR));

            PlacedFeature placedFeature = new PlacedFeature(configuredFeaturesRegistry.getHolderOrThrow(configuredKey),modifiers);



            ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(false);
            ((MappedRegistry<PlacedFeature>) placedFeatureRegistry).register(
                    placedKey,
                    placedFeature,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(true);
            features.add(placedKey);
        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeDelta(CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<ConfiguredFeature<?,?>> configuredFeaturesRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.getByteArray("flavour")[3]==1 && biomeData.getInt("temperature")>500)
        {
            ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeaturesRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_delta"));
            ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_delta"));
            if(!placedFeatureRegistry.containsKey(placedKey))
            {
                DeltaFeatureConfiguration configuration = new DeltaFeatureConfiguration(Blocks.LAVA.defaultBlockState(),Blocks.MAGMA_BLOCK.defaultBlockState(), UniformInt.of(3,7),UniformInt.of(0,2));
                ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.DELTA_FEATURE,configuration);



                ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(false);
                ((MappedRegistry<ConfiguredFeature<?,?>>) configuredFeaturesRegistry).register(
                        configuredKey,
                        feature,
                        Lifecycle.stable() // use built-in registration info for now
                );
                ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(true);

                List<PlacementModifier> modifiers = new ArrayList<>();
                modifiers.add(CountOnEveryLayerPlacement.of(40));
                PlacedFeature placedFeature = new PlacedFeature(configuredFeaturesRegistry.getHolderOrThrow(configuredKey),modifiers);



                ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(false);
                ((MappedRegistry<PlacedFeature>) placedFeatureRegistry).register(
                        placedKey,
                        placedFeature,
                        Lifecycle.stable() // use built-in registration info for now
                );
                ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(true);
                features.add(placedKey);
            }

        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeOres(CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<ConfiguredFeature<?,?>> configuredFeaturesRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("biome_ores"))
        {
            ListTag ores = (ListTag) biomeData.get("biome_ores");
            for (int i = 0; i < ores.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeaturesRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_ore_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_ore_"+i));
                if(!placedFeatureRegistry.containsKey(placedKey))
                {
                    BlockState oreState = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(ores.getString(i))).defaultBlockState();
                    OreConfiguration configuration = new OreConfiguration(List.of(OreConfiguration.target(new TagMatchTest(BlockTags.DIRT),oreState)),10);
                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(OreFeature.ORE,configuration);



                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) configuredFeaturesRegistry).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(CountPlacement.of(16));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-11),VerticalAnchor.belowTop(112)));
                    PlacedFeature placedFeature = new PlacedFeature(configuredFeaturesRegistry.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) placedFeatureRegistry).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(true);
                    features.add(placedKey);
                }



            }
        }
        return features;
    }
    public static List<ResourceKey<PlacedFeature>> makeLakes(CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        Registry<ConfiguredFeature<?,?>> configuredFeaturesRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("lakeFluids"))
        {
            ListTag lakes = (ListTag) biomeData.get("lakeFluids");
            BlockState barrier = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(biomeData.getString("generalBlock"))).defaultBlockState();
            for (int i = 0; i < lakes.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeaturesRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_lake_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_lake_"+i));
                if(!placedFeatureRegistry.containsKey(placedKey))
                {
                    BlockState fluid = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(lakes.getString(i))).defaultBlockState();

                    LakeFeature.Configuration lakeConfig = new LakeFeature.Configuration(BlockStateProvider.simple(fluid),BlockStateProvider.simple(barrier));

                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.LAKE,lakeConfig);



                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) configuredFeaturesRegistry).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(RarityFilter.onAverageOnceEvery(200));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG));
                    PlacedFeature placedFeature = new PlacedFeature(configuredFeaturesRegistry.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) placedFeatureRegistry).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(true);
                    features.add(placedKey);
                }



            }
        }
        return features;
    }

    public static List<ResourceKey<PlacedFeature>> makeRocks(CompoundTag biomeData, RegistryAccess.Frozen access)
    {
        Registry<ConfiguredFeature<?,?>> configuredFeaturesRegistry = access.registryOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedFeatureRegistry = access.registryOrThrow(Registries.PLACED_FEATURE);
        List<ResourceKey<PlacedFeature>> features = new ArrayList<>();
        if(biomeData.contains("rock_blocks"))
        {
            ListTag rocks = (ListTag) biomeData.get("rock_blocks");
            for (int i = 0; i < rocks.size(); i++) {
                ResourceKey<ConfiguredFeature<?,?>> configuredKey = ResourceKey.create(configuredFeaturesRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_rock_"+i));
                ResourceKey<PlacedFeature> placedKey = ResourceKey.create(placedFeatureRegistry.key(),ResourceLocation.tryBuild("dataplanets",biomeData.getString("name")+"_rock_"+i));
                if(!placedFeatureRegistry.containsKey(placedKey))
                {
                    BlockState rock = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(rocks.getString(i))).defaultBlockState();

                    ConfiguredFeature<?,?> feature = new ConfiguredFeature<>(Feature.FOREST_ROCK,new BlockStateConfiguration(rock));


                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(false);
                    ((MappedRegistry<ConfiguredFeature<?,?>>) configuredFeaturesRegistry).register(
                            configuredKey,
                            feature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) configuredFeaturesRegistry).setRegFrozen(true);

                    List<PlacementModifier> modifiers = new ArrayList<>();
                    modifiers.add(RarityFilter.onAverageOnceEvery(2));
                    modifiers.add(InSquarePlacement.spread());
                    modifiers.add(HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING));
                    PlacedFeature placedFeature = new PlacedFeature(configuredFeaturesRegistry.getHolderOrThrow(configuredKey),modifiers);



                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(false);
                    ((MappedRegistry<PlacedFeature>) placedFeatureRegistry).register(
                            placedKey,
                            placedFeature,
                            Lifecycle.stable() // use built-in registration info for now
                    );
                    ((IUnfreezableRegistry) placedFeatureRegistry).setRegFrozen(true);
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


    public static LevelStem makeStar(CompoundTag systemData, RegistryAccess.Frozen access)
    {
        Registry<DimensionType> dimensionRegistry = access.registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        Registry<LevelStem> levelStemRegistry = access.registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> starKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets", systemData.getString("systemName")));
        if(!levelStemRegistry.containsKey(starKey))
        {
            Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Compat.SPACE_BIOME);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(),biomeHolder,List.of())
                    .withBiomeAndLayers(List.of(new FlatLayerInfo(1,Blocks.BEDROCK),new FlatLayerInfo(50,Blocks.LAVA),new FlatLayerInfo(50, DPBlocks.DENSE_GAS.get())),Optional.empty(),biomeHolder);
            Holder.Reference<DimensionType> holder = dimensionRegistry.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);

            FlatLevelSource flatLevelSource = new FlatLevelSource(settings);
            LevelStem stem = new LevelStem(holder,flatLevelSource);

            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) levelStemRegistry).register(
                    starKey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );
        }
        return levelStemRegistry.get(starKey);
    }

    public static LevelStem makeGasPlanet(CompoundTag planetData, RegistryAccess.Frozen access)
    {
        Registry<DimensionType> dimensionRegistry = access.registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        Registry<LevelStem> levelStemRegistry = access.registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> starKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets", planetData.getString("name")));
        if(!levelStemRegistry.containsKey(starKey))
        {
            Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Compat.SPACE_BIOME);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(),biomeHolder,List.of())
                    .withBiomeAndLayers(List.of(new FlatLayerInfo(1,Blocks.BEDROCK),new FlatLayerInfo(50,Blocks.LAVA),new FlatLayerInfo(50, DPBlocks.DENSE_GAS.get())),Optional.empty(),biomeHolder);
            Holder.Reference<DimensionType> holder = dimensionRegistry.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);

            FlatLevelSource flatLevelSource = new FlatLevelSource(settings);
            LevelStem stem = new LevelStem(holder,flatLevelSource);

            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) levelStemRegistry).register(
                    starKey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );
        }
        Compat.postLoadPlanet(planetData);
        return levelStemRegistry.get(starKey);
    }

    public static LevelStem makePlanet(CompoundTag planetData, RegistryAccess.Frozen access)
    {
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        Registry<NormalNoise.NoiseParameters> noiseRegistry = access.registryOrThrow(Registries.NOISE);
        Registry<DimensionType> dimensionRegistry = access.registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<LevelStem> levelStemRegistry = access.registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> resourcekey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",planetData.getString("name")));
        Holder.Reference<DimensionType> holder = dimensionRegistry.getHolderOrThrow(makeDimType(planetData, access));
        if(!levelStemRegistry.containsKey(resourcekey))
        {

            Holder<NormalNoise.NoiseParameters> offset = noiseRegistry.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","offset")));

            NoiseGeneratorSettings settings = new NoiseGeneratorSettings(
                    NoiseSettings.create(-64, 384, 2, 2),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(planetData.getString("generalBlock"))).defaultBlockState(),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(planetData.getString("seaBlock"))).defaultBlockState(),
                    getNoiseRouter(offset, planetData, access),
                    planetarySurfaceRuleSource(planetData.getBoolean("hasOxygen")&&planetData.getBoolean("hasAtmosphere")),
                    new OverworldBiomeBuilder().spawnTarget(),
                    planetData.getInt("seaLevel"),
                    false,
                    true,
                    true,
                    false
            );
            //Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(makeBiome(planetData));

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
                        ),biomeRegistry.getHolderOrThrow(makeBiome(b, access))));
            }

            MultiNoiseBiomeSource n = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomes));
            NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(n, Holder.direct(settings));
            LevelStem stem = new LevelStem(holder,noiseBasedChunkGenerator);

            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) levelStemRegistry).register(
                    resourcekey,
                    stem,
                    Lifecycle.stable() // use built-in registration info for now
            );

            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(true);

        }

        Compat.postLoadPlanet(planetData);
        return levelStemRegistry.get(resourcekey);
    }

    private static NoiseRouter getNoiseRouter(Holder<NormalNoise.NoiseParameters> offset, CompoundTag planetData, RegistryAccess.Frozen access) {

        Registry<NormalNoise.NoiseParameters> noiseRegistry = access.registryOrThrow(Registries.NOISE);

        DensityFunction finalDensity = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1, -1),
                DensityFunctions.noise(noiseRegistry.getHolderOrThrow(Noises.GRAVEL), planetData.getFloat("nr1"), planetData.getFloat("nr2")));

        return new NoiseRouter(
                DensityFunctions.constant(0),
                DensityFunctions.constant(0),
                DensityFunctions.constant(0),
                DensityFunctions.constant(0),
                DensityFunctions.shiftedNoise2d(
                        DensityFunctions.shiftA(offset),
                        DensityFunctions.shiftB(offset),
                        0.25F,
                        noiseRegistry.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","temperature")))
                ),
                DensityFunctions.shiftedNoise2d(
                        DensityFunctions.shiftA(offset),
                        DensityFunctions.shiftB(offset),
                        0.25F,
                        noiseRegistry.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","vegetation")))
                ),
                DensityFunctions.constant(0),
                DensityFunctions.shiftedNoise2d(
                        DensityFunctions.shiftA(offset),
                        DensityFunctions.shiftB(offset),
                        0.25F,
                        noiseRegistry.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","erosion")))
                ),
                DensityFunctions.constant(0),
                DensityFunctions.shiftedNoise2d(
                        DensityFunctions.shiftA(offset),
                        DensityFunctions.shiftB(offset),
                        0.25F,
                        noiseRegistry.getHolderOrThrow(ResourceKey.create(Registries.NOISE,ResourceLocation.fromNamespaceAndPath("minecraft","ridge")))
                ),
                DensityFunctions.constant(0),
                finalDensity,
                DensityFunctions.constant(0),
                DensityFunctions.constant(0),
                DensityFunctions.constant(0)
        );
    }

    public static LevelStem makeOrbit(CompoundTag planetData, RegistryAccess.Frozen access)
    {
        Registry<LevelStem> levelStemRegistry = access.registryOrThrow(Registries.LEVEL_STEM);
        Registry<DimensionType> dimensionRegistry = access.registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        ResourceKey<LevelStem> orbitKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",planetData.getString("name")+"_orbit"));

        if(!levelStemRegistry.containsKey(orbitKey))
        {
            Holder.Reference<DimensionType> orbitHolder = dimensionRegistry.getHolderOrThrow(Compat.SPACE_DIMENSION_TYPE);
            Holder.Reference<Biome> orbitBiomeHolder = biomeRegistry.getHolderOrThrow(Compat.SPACE_BIOME);

            LevelStem orbit = new LevelStem(orbitHolder,Compat.spaceGenerator(orbitBiomeHolder));

            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(false);
            ((MappedRegistry<LevelStem>) levelStemRegistry).register(
                    orbitKey,
                    orbit,
                    Lifecycle.stable() // use built-in registration info for now
            );
            ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(true);
            return orbit;
        }
        return levelStemRegistry.get(orbitKey);


    }

    public static void makeDynamicWorld(String name, LevelStem stem, DimensionDataStorage storage)
    {

//        if(!((MinecraftServerAccessor) server).getLevels().containsKey(dimensionKey))
//        {
        DimensionManager.INSTANCE.queueLevelForRegistration(ResourceLocation.tryBuild("dataplanets",name),stem);
        System.out.println(name);
//            ChunkProgressListener listener = server.progressListenerFactory.create(server.getWorldData().getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS));
//
//            ServerLevelData serverleveldata = server.getWorldData().overworldData();
//
//            DerivedLevelData derivedleveldata = new DerivedLevelData(server.getWorldData(), serverleveldata);
//            ServerLevel serverlevel1 = new ServerLevel(server, Util.backgroundExecutor(), server.storageSource, derivedleveldata, dimensionKey, stem, listener, server.getWorldData().isDebugWorld(), BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()), ImmutableList.of(), false, server.overworld().getRandomSequences());
//            server.overworld().getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(serverlevel1.getWorldBorder()));
//
//            server.levels.put(dimensionKey, serverlevel1);
//            MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(server.levels.get(dimensionKey)));
        TaskUtil.queueTickStart(() -> {
            Compat.postLoadWorld(ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage());
        });

//        }
    }

    public static void generateNewSystem(CompoundTag systemData, String systemId, RegistryAccess.Frozen access, DimensionDataStorage storage){
        makeDynamicWorld(systemId,makeStar(systemData, access), storage);

        for(String planetId: systemData.getAllKeys())
        {
            CompoundTag planetData = systemData.getCompound(planetId);
            if(systemData.getTagType(planetId)== Tag.TAG_COMPOUND)
            {
                System.out.println("creating planet: "+planetId);
                if(planetData.contains("planetType") && planetData.getString("planetType").equals("gaseous"))
                {
                    makeDynamicWorld(planetId,makeGasPlanet(planetData, access), storage);
                }
                else
                {
                    makeDynamicWorld(planetId, makePlanet(planetData, access), storage);
                }

                //makeDynamicWorld(server,planetId+"_orbit",makeOrbit(planetData));
            }
        }
    }
}
