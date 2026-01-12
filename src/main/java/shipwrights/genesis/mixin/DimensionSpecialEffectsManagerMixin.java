package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.DimensionSpecialEffectsManager;
import org.spongepowered.asm.mixin.Mixin;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;

@Mixin(value = DimensionSpecialEffectsManager.class, remap = false)
public class DimensionSpecialEffectsManagerMixin {

    @WrapMethod(method = "getForType")
    private static DimensionSpecialEffects wrapGetForType(ResourceLocation type, Operation<DimensionSpecialEffects> original) {
        if (type.equals(ResourceLocation.parse("minecraft:overworld"))) {
            return new PlanetDimensionEffects(GenesisMod.SPACE_REGISTRY);
        }
        return original.call(type);
    }
}
