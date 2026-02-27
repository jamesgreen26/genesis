package shipwrights.genesis.mixin;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor
    ClientLevel getLevel();
    
    @Accessor
    VertexBuffer getSkyBuffer();
}
