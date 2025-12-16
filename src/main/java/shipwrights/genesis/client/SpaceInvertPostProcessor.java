package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import team.lodestar.lodestone.systems.postprocess.PostProcessor;

public class SpaceInvertPostProcessor extends PostProcessor {
    public static final SpaceInvertPostProcessor INSTANCE = new SpaceInvertPostProcessor();

    @Override
    public ResourceLocation getPostChainLocation() {
        return ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space_invert");
    }

    @Override
    public void init() {
        super.init();
        // Register custom samplers with the effect instances
        if (effects != null) {
            for (EffectInstance effect : effects) {
                effect.setSampler("PlanetMaskSampler", PlanetMaskTarget::getColorTextureId);
                effect.setSampler("PlanetDepthSampler", PlanetMaskTarget::getDepthTextureId);
            }
        }
    }

    @Override
    public void beforeProcess(PoseStack viewModelStack) {
        // Samplers are now set up in init() and will be automatically bound
    }

    @Override
    public void afterProcess() {
        // Samplers are handled automatically by Minecraft's EffectInstance
    }
}
