package shipwrights.genesis.space.renderer;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

public interface CelestialRenderer {
    void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint);

    default void setup(@NotNull RenderLevelStageEvent event, @NotNull VantagePoint vantagePoint) {}

    default void teardown(@NotNull RenderLevelStageEvent event, @NotNull VantagePoint vantagePoint) {}
}
