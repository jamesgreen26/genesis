package shipwrights.genesis.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;

/// Tangled in webbing: movement is heavily slowed and jumps reach half as high
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public class WebbedEffect extends MobEffect {
    private static final ResourceLocation SPEED_MODIFIER_ID = GenesisMod.id("webbed_slowness");
    /// Scale on jump speed that halves jump height. Height would go with speed squared (1/√2), but air drag bleeds off
    /// more of a slower jump, so it's found numerically instead. Holds within a few percent from moon to overworld
    /// gravity
    private static final double JUMP_SPEED_SCALE = 0.67;

    public WebbedEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8EEF2);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /// Fired on both sides right after the jump speed is set, so players' client-side jumps are cut too
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(GenesisMobEffects.WEBBED)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, JUMP_SPEED_SCALE, 1));
        }
    }
}
