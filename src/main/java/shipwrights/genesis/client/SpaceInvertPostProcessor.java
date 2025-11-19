package shipwrights.genesis.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
    public void beforeProcess(PoseStack viewModelStack) {
        // Shader activation is controlled by SpaceShaderEventHandler
    }

    @Override
    public void afterProcess() {
        // No cleanup needed for this simple shader
    }
}
