package shipwrights.genesis.space.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.type.CelestialType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CelestialRenderDispatcher {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        Celestial vantagePoint = GenesisMod.getCelestialForLevel(level);

        if (vantagePoint != null || GenesisMod.isSpaceDimension(level)) {
            // Group celestials by type for organized rendering
            Map<CelestialType, List<Celestial>> celestialsByType = GenesisMod.SPACE_REGISTRY.getAll().stream()
                    .collect(Collectors.groupingBy(Celestial::getType));

            // Render each type group
            for (Map.Entry<CelestialType, List<Celestial>> entry : celestialsByType.entrySet()) {
                CelestialType type = entry.getKey();
                List<Celestial> celestials = entry.getValue();

                // Setup before rendering this type group
                type.getRenderer().setup(event, vantagePoint);

                // Render all celestials of this type
                for (Celestial celestial : celestials) {
                    type.getRenderer().invoke(event, celestial, vantagePoint);
                }

                // Teardown after rendering this type group
                type.getRenderer().teardown(event, vantagePoint);
            }
        }
    }
}
