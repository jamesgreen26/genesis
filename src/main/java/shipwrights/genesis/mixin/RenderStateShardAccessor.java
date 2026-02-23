package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface RenderStateShardAccessor {
    @Accessor
    static RenderStateShard.OutputStateShard getTRANSLUCENT_TARGET() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static RenderStateShard.LayeringStateShard getVIEW_OFFSET_Z_LAYERING() {
        throw new UnsupportedOperationException();
    }
}