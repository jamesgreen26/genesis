package shipwrights.genesis.space.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

/// A shader with blending of its own, set from code each time it's applied rather than by the `blend` of its JSON.
///
/// Vanilla remembers the last `blend` it applied, and skips applying one equal to it. Every vanilla shader declares the
/// same one, so in practice none is ever applied, and the blending comes from the render type instead. A shader
/// declaring a different `blend` can end up as the remembered one (it does whenever it follows a shader with none),
/// after which every vanilla shader applies its `blend` on every draw, over the blending of its render type: the
/// enchantment glint is drawn opaque, for one.
///
/// So the JSON of every Genesis shader declares vanilla's `blend`, and those that need another set it here.
public final class BlendedShaderInstance extends ShaderInstance {
    private final Runnable blending;

    /// @param blending sets up the blending, as by `RenderSystem.blendFunc`
    public BlendedShaderInstance(ResourceProvider resourceProvider, ResourceLocation location, VertexFormat vertexFormat,
                                 Runnable blending) throws IOException {
        super(resourceProvider, location, vertexFormat);
        this.blending = blending;
    }

    @Override
    public void apply() {
        // May apply the blend of the JSON
        super.apply();
        blending.run();
    }
}
