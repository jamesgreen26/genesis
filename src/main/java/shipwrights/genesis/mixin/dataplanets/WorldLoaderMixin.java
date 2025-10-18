package shipwrights.genesis.mixin.dataplanets;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import shipwrights.dataplanets.mixinducks.PackRepositoryDuck;
import shipwrights.dataplanets.space.DynamicSystems;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;getFirst()Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private static <D, R> void goo(WorldLoader.InitConfig pInitConfig, WorldLoader.WorldDataSupplier<D> dataSupplier, WorldLoader.ResultFactory<D, R> arg2, Executor arg3, Executor arg4, CallbackInfoReturnable<CompletableFuture<R>> cir, Pair<WorldDataConfiguration, CloseableResourceManager> pair, CloseableResourceManager closeableresourcemanager, LayeredRegistryAccess<RegistryLayer> layeredregistryaccess, LayeredRegistryAccess<RegistryLayer> layeredregistryaccess1, RegistryAccess.Frozen registryaccessfrozen, RegistryAccess.Frozen registryaccessfrozen1){
        System.out.println("hi dfsf");
        if (pInitConfig.packConfig().initMode()){
            DynamicSystems.DATA_PROVIDER = null;
            return;
        }

        File dataPath = ((PackRepositoryDuck) pInitConfig.packConfig().packRepository()).dataplanets$getPath().toFile();
        dataPath = Path.of(dataPath.toString().substring(0, dataPath.toString().length() - (LevelResource.DATAPACK_DIR.getId().length()+1))).resolve("data").toFile();
        System.out.println(dataPath);
        dataPath.mkdirs();

        DynamicSystems.DATA_PROVIDER = new DimensionDataStorage(dataPath, DataFixers.getDataFixer());
    }
}
