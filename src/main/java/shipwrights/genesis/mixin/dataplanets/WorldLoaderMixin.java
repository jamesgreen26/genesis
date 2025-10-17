package shipwrights.genesis.mixin.dataplanets;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.dataplanets.mixinducks.PackRepositoryDuck;
import shipwrights.dataplanets.space.DynamicSystems;
import shipwrights.dataplanets.util.TaskUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;", shift = At.Shift.AFTER))
    private static <D, R> void mixin(WorldLoader.InitConfig pInitConfig, WorldLoader.WorldDataSupplier<D> dataSupplier, WorldLoader.ResultFactory<D, R> arg2, Executor arg3, Executor arg4, CallbackInfoReturnable<CompletableFuture<R>> cir, @Local(ordinal = 1) RegistryAccess.Frozen registryaccessfrozen, @Local(ordinal = 0) CloseableResourceManager closeableresourcemanager, @Local(ordinal = 1) LayeredRegistryAccess<RegistryLayer> layeredregistryaccess){
        System.out.println("hi dfsf");

        if (pInitConfig.packConfig().initMode()){
            TaskUtil.queueTickStart(() -> {
                System.out.println("TICK START REGISTRATION GOES MEOW MEOW MEOW");
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                DynamicSystems.loadDynamicResources(server.registryAccess(), server.overworld().getDataStorage());
            });
            return;
        }

        File dataPath = ((PackRepositoryDuck) pInitConfig.packConfig().packRepository()).dataplanets$getPath().toFile();
        dataPath = Path.of(dataPath.toString().substring(0, dataPath.toString().length() - (LevelResource.DATAPACK_DIR.getId().length()+1))).resolve("data").toFile();
        System.out.println(dataPath);
        dataPath.mkdirs();

        RegistryAccess.Frozen registryaccessfrozen2 = layeredregistryaccess.getAccessForLoading(RegistryLayer.RELOADABLE);

        System.out.println(registryaccessfrozen.registries().collect(Collectors.toSet()));



        registryaccessfrozen2 = RegistryDataLoader.load(closeableresourcemanager,registryaccessfrozen2, List.of(
//                new RegistryDataLoader.RegistryData(Registries.BLOCK, BuiltInRegistries.BLOCK.byNameCodec()),
                new RegistryDataLoader.RegistryData(Registries.DIMENSION_TYPE, DimensionType.CODEC),
                new RegistryDataLoader.RegistryData(Registries.BIOME, Biome.CODEC),
                new RegistryDataLoader.RegistryData(Registries.PLACED_FEATURE, PlacedFeature.CODEC),
                new RegistryDataLoader.RegistryData(Registries.CONFIGURED_FEATURE, ConfiguredFeature.CODEC),
                new RegistryDataLoader.RegistryData(Registries.CONFIGURED_CARVER, ConfiguredWorldCarver.CODEC),
                new RegistryDataLoader.RegistryData(Registries.NOISE, NormalNoise.NoiseParameters.CODEC)
                )
        );

        Stream<RegistryAccess.RegistryEntry<?>> regs = Stream.concat(registryaccessfrozen2.registries().filter((registryEntry) -> registryEntry.key() != Registries.BLOCK), Stream.of(new RegistryAccess.RegistryEntry<>(Registries.BLOCK, BuiltInRegistries.BLOCK)));
        regs = Stream.concat(regs.filter((registryEntry) -> registryEntry.key() != Registries.DIMENSION), Stream.of(new RegistryAccess.RegistryEntry<>(Registries.DIMENSION, registryaccessfrozen.registryOrThrow(Registries.DIMENSION))));


        registryaccessfrozen2 = new RegistryAccess.ImmutableRegistryAccess(regs).freeze();
        System.out.println(registryaccessfrozen2.registries().collect(Collectors.toSet()));

//        LevelStorageSource.LevelStorageAccess levelStorageAccess = ((PackRepositoryDuck) pInitConfig.packConfig().packRepository()).getLevelAccess();
//        pInitConfig.packConfig().packRepository()
//        File dataPath = levelStorageAccess.getDimensionPath(Level.OVERWORLD).resolve("data").toFile();
//        dataPath.mkdirs();

        DimensionDataStorage storage = new DimensionDataStorage(dataPath, DataFixers.getDataFixer());

//        DynamicSavedData data = storage.computeIfAbsent(DynamicSavedData::new, DynamicSavedData::new, "dataplanets_dynamic_data");

//        Registry<Biome> biomeRegistry = registryaccessfrozen.registryOrThrow(Registries.BIOME);
        DynamicSystems.loadDynamicResources(registryaccessfrozen2, storage);
    }
}
