package shipwrights.genesis.space.renderer;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.space.Celestial;

public interface CelestialRenderer {
    void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @Nullable Celestial vantagePoint);

    default void setup(@NotNull RenderLevelStageEvent event, @Nullable Celestial vantagePoint) {}

    default void teardown(@NotNull RenderLevelStageEvent event, @Nullable Celestial vantagePoint) {}
}
