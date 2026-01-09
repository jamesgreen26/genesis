package shipwrights.dataplanets.systemCreation;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import net.minecraft.world.level.Level;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.genesis.mixin.MinecraftServerAccessor;


/**
 * Props to infiniverse
 */
public class DimensionManager {
    public static DimensionManager INSTANCE = new DimensionManager();

    private ConcurrentLinkedQueue<Map.Entry<ResourceKey<Level>, LevelStem>> registrationQueue = new ConcurrentLinkedQueue<>();

    private DimensionManager(){}

    @SuppressWarnings("deprecation")
    private void createAndRegisterLevel(MinecraftServer server, ResourceKey<Level> levelKey, final LevelStem levelStem){

        Map<ResourceKey<Level>, ServerLevel> map = server.forgeGetWorldMap();

        // dimension key location is key is same as level key
        final ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        final ResourceKey<LevelStem> dimensionKey = ResourceKey.create(Registries.LEVEL_STEM, levelKey.location());


        final ChunkProgressListener chunkProgressListener = ((MinecraftServerAccessor)server).getProgressListenerFactory().create(11); // chunk watch radius, 11 is default
        final Executor executor = ((MinecraftServerAccessor)server).getExecutor();
        final LevelStorageSource.LevelStorageAccess anvilConverter = ((MinecraftServerAccessor)server).getStorageSource();
        final WorldData worldData = server.getWorldData();
        final DerivedLevelData derivedLevelData = new DerivedLevelData(worldData, worldData.overworldData());


        Registry<LevelStem> dimensionRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        if (dimensionRegistry instanceof MappedRegistry<LevelStem> writableRegistry)
        {
            writableRegistry.unfreeze();
            writableRegistry.register(dimensionKey, levelStem, Lifecycle.stable());
        }
        else
        {
            throw new IllegalStateException(String.format("Unable to register dimension %s -- dimension registry not writable", dimensionKey.location()));
        }


        // create the level instance
        final ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                anvilConverter,
                derivedLevelData,
                levelKey,
                levelStem,
                chunkProgressListener,
                worldData.isDebugWorld(),
                overworld.getSeed(), // no need to obfuscate cuz overworld seed is already obfuscated
                List.of(), // special spawn list
                false, // spawn chunks
                null // level should load the sequence from storage
        );

        overworld.getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder()));

        // register the level
        map.put(levelKey, newLevel);

        // world cache update
        server.markWorldsDirty();

        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));
//        DPPackets.sendToAll(DPPackets.INSTANCE,new UpdateDimensionsPacket(Set.of(levelKey), true));
    }

    public void queueLevelForRegistration(ResourceKey<Level> levelKey, LevelStem stem){
        this.registrationQueue.add(new AbstractMap.SimpleEntry<>(levelKey, stem));
    }

    @SuppressWarnings("deprecation")
    private void registerQueuedLevels(MinecraftServer server){
        while (!registrationQueue.isEmpty()){
            Map.Entry<ResourceKey<Level>, LevelStem> element = registrationQueue.poll();

            // if the world already exists, do nuffin
            if (server.forgeGetWorldMap().get(element.getKey()) != null){
                return;
            }

            this.createAndRegisterLevel(server, element.getKey(), element.getValue());
        }
    }

    @Mod.EventBusSubscriber(modid = DataplanetsMod.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onServerTick(final TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    DimensionManager.INSTANCE.registerQueuedLevels(server);
                }
            }
        }
    }

}
