package shipwrights.genesis.space.renderer;

import dev.engine_room.flywheel.api.visual.Effect;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.Celestial;

public interface EffectFactory {
    @NotNull Effect getEffect(Celestial celestial, Level level);
}
