package shipwrights.genesis.mixin.dataplanets;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.dataplanets.MutableTags;
import shipwrights.dataplanets.PlanetLookup;
import shipwrights.dataplanets.items.SpaceArmourItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.dataplanets.systemCreation.PlanetData;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow @Nullable public abstract AttributeInstance getAttribute(Attribute p_21052_);

    @Unique
    private static final UUID dataplanets$lowGravity = UUID.randomUUID();

    @Unique
    private static boolean dataplanets$shouldBurn(double temperature) {
        return temperature > 1.6;
    }

    @Unique
    private static boolean dataplanets$shouldFreeze(double temperature) {
        return temperature < 0.4;
    }

    @Unique
    private static boolean dataplanets$shouldSuffocate(double atmosphericDensity) {
        return atmosphericDensity < 0.4;
    }

    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Inject(method = "equipmentHasChanged", at = @At("TAIL"))
    private void equip(ItemStack oldItem, ItemStack newItem, CallbackInfoReturnable<Boolean> cir)
    {
        if(oldItem.is(MutableTags.PROVIDES_OXYGEN))
        {
            removeTag("has_oxygen");
        }
        if(newItem.is(MutableTags.PROVIDES_OXYGEN))
        {
            addTag("has_oxygen");
        }
        if(oldItem.getItem() instanceof SpaceArmourItem)
        {
            if(SpaceArmourItem.hasModule(oldItem,"heat"))
            {
                removeTag("has_heat");
            }
            if(SpaceArmourItem.hasModule(newItem,"heat"))
            {
                addTag("has_heat");
            }
            if(SpaceArmourItem.hasModule(oldItem,"cooling"))
            {
                removeTag("has_cooling");
            }
            if(SpaceArmourItem.hasModule(newItem,"cooling"))
            {
                addTag("has_cooling");
            }
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    private void checkKey(CallbackInfo ci)
    {
        AttributeInstance gravity = this.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if(this.level().dimension().location().getNamespace().equals("dataplanets")) {
            ResourceLocation dimension = this.level().dimension().location();
            PlanetData planetData = PlanetLookup.get(dimension);

            if(gravity != null) {
                double mult = planetData.gravity() - 1.0;

                AttributeModifier modifierNew = new AttributeModifier(dataplanets$lowGravity, "Low Gravity", mult, AttributeModifier.Operation.MULTIPLY_TOTAL);

                AttributeModifier modifierOld = gravity.getModifier(dataplanets$lowGravity);
                if(modifierOld == null || modifierOld.getAmount() != modifierNew.getAmount()) {
                    gravity.removeModifier(dataplanets$lowGravity);
                    gravity.addPermanentModifier(modifierNew);
                }

            }
            final MinecraftServer server = this.getServer();
            boolean creative = (LivingEntity) (Object) this instanceof Player player && player.isCreative();

            if(!level().isClientSide && server != null && !creative)
            {
                if(dataplanets$shouldSuffocate(planetData.atmosphericDensity()))
                {
                    if(getTags().contains("in_oxygen_bubble"))
                    {
                        removeTag("in_oxygen_bubble");
                    }
                    else if(!getTags().contains("has_oxygen"))
                    {
                        hurt(damageSources().drown(),1);
                    }
                }
                if(dataplanets$shouldFreeze(planetData.temperature()))
                {
                    if(!getTags().contains("has_heat"))
                    {
                        setTicksFrozen(200);
                    }
                }
                if(dataplanets$shouldBurn(planetData.temperature()))
                {
                    if(!getTags().contains("has_cooling"))
                    {
                        setRemainingFireTicks(20);
                    }
                }
            }

        }
        else
        {
            if(gravity!=null)
            {
                gravity.removeModifier(dataplanets$lowGravity);
            }
        }
    }
}
