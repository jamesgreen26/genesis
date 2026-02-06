package shipwrights.genesis.space.renderer;

import dev.engine_room.flywheel.api.event.ReloadLevelRendererEvent;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.resources.ResourceKey;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CelestialRenderDispatcher {

    private static final Map<ResourceKey<Level>, Queue<Effect>> activeEffectsByDimension = new ConcurrentHashMap<>();


    @SubscribeEvent
    public static void onClientLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level) {
            removeAllCelestials(level);
            addCelestials(level);
        }
    }

    @SubscribeEvent
    public static void onReloadRenderers(ReloadLevelRendererEvent event) {
        removeAllCelestials(event.level());
        addCelestials(event.level());
    }

    @SubscribeEvent
    public static void onLevelUnloadEvent(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            removeAllCelestials(level);
        }
    }

    private static void addCelestials(Level level) {
        VisualizationManager visualizationManager = FlwApiLink.INSTANCE.getVisualizationManager(level);
        if (visualizationManager == null) return;

        VisualManager<Effect> visualManager = visualizationManager.effects();

        if (GenesisMod.getCelestialForLevel(level) != null || GenesisMod.isSpaceDimension(level)) {
            ResourceKey<Level> dimensionKey = level.dimension();
            Queue<Effect> effectQueue = activeEffectsByDimension.computeIfAbsent(dimensionKey, k -> new ConcurrentLinkedQueue<>());

            for (Celestial celestial : GenesisMod.SPACE_REGISTRY.getAll()) {
                EffectFactory effectFactory = celestial.getType().getEffectFactory();
                if (effectFactory == null) continue;
                Effect effect = effectFactory.getEffect(celestial, level);
                effectQueue.add(effect);
                visualManager.queueAdd(effect);
            }
        }
    }

    private static void removeAllCelestials(Level level) {
        VisualizationManager visualizationManager = FlwApiLink.INSTANCE.getVisualizationManager(level);
        if (visualizationManager == null) return;

        VisualManager<Effect> visualManager = visualizationManager.effects();

        ResourceKey<Level> dimensionKey = level.dimension();
        Queue<Effect> effectQueue = activeEffectsByDimension.remove(dimensionKey);

        if (effectQueue != null) {
            while (!effectQueue.isEmpty()) {
                visualManager.queueRemove(effectQueue.poll());
            }
        }
    }
}
