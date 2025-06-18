package g_mungus.genesis.mixin;

import g_mungus.genesis.GenesisMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow public abstract ServerLevel getLevel();

    @Unique
    private boolean genesis$isSpace() {
        return ((ServerLevel)(Object) this).getLevel().dimension().location().equals(GenesisMod.SPACE_DIM);
    }

    @Unique
    private ResourceLocation genesis$entityScale = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "entity_scale");

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void addEntityMixin(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        genesis$scaleEntity(entity);
    }

    @Inject(method = "addPlayer", at = @At("HEAD"))
    private void addPlayerMixin(ServerPlayer arg, CallbackInfo ci) {
        genesis$scaleEntity(arg);
    }

    @Unique
    private void genesis$scaleEntity(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            if (genesis$isSpace()) {
                AttributeInstance scale = livingEntity.getAttributes().getInstance(Attributes.SCALE);
                if (scale != null) {
                    AttributeModifier attributeModifier = new AttributeModifier(genesis$entityScale, -15.0 / 16.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                    scale.addOrReplacePermanentModifier(attributeModifier);
                }

            } else {
                AttributeInstance scale = livingEntity.getAttributes().getInstance(Attributes.SCALE);
                if (scale != null) {
                    scale.removeModifier(genesis$entityScale);
                }
            }
        }
    }
}
