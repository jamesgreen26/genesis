package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FogRenderer.class)
public interface FogRendererAccessor {
    @Accessor
    static float getFogRed() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static float getFogGreen() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static float getFogBlue() {
        throw new UnsupportedOperationException();
    }
}
