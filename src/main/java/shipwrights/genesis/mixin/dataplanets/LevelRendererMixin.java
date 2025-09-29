package shipwrights.genesis.mixin.dataplanets;

import shipwrights.dataplanets.space.DynamicSystems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = "renderSnowAndRain", at = @At(value="INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V", shift = At.Shift.AFTER))
    public void rainy(LightTexture p_109704_, float p_109705_, double p_109706_, double p_109707_, double p_109708_, CallbackInfo ci)
    {
        ResourceKey<Biome> b = level.getBiome(Minecraft.getInstance().player.getOnPos()).unwrapKey().get();
        String full = b.location().getPath().replace("_terrain","");
        if(DynamicSystems.RAIN_COLOUR.containsValue(full))
        {
            float[] a = DynamicSystems.RAIN_COLOUR.get(full);
            RenderSystem.setShaderColor(a[0],a[1],a[2],a[3]);
        }


    }
    @Inject(method="renderSnowAndRain",at = @At("TAIL"))
    public void notColour(LightTexture p_109704_, float p_109705_, double p_109706_, double p_109707_, double p_109708_, CallbackInfo ci)
    {
        RenderSystem.setShaderColor(1,1,1,1);
    }
}
