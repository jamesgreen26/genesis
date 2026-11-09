package shipwrights.genesis.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/// Tangled in webbing: movement is heavily slowed
public class WebbedEffect extends MobEffect {
    private static final String SPEED_MODIFIER_UUID = "5b7e8a1c-3f2d-4c6e-9a8b-1d2e3f4a5b6c";

    public WebbedEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8EEF2);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID, -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
