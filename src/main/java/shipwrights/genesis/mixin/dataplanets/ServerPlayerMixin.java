package shipwrights.genesis.mixin.dataplanets;

import shipwrights.dataplanets.Dataplanets;
import shipwrights.dataplanets.space.StarSystemCreator;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class ServerPlayerMixin extends Entity {

    @Shadow @Nullable public abstract AttributeInstance getAttribute(Attribute p_21052_);


    @Shadow
    public abstract boolean addEffect(MobEffectInstance p_21165_);

    public ServerPlayerMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Inject(method = "equipmentHasChanged", at = @At("TAIL"))
    private void equip(ItemStack oldItem, ItemStack newItem, CallbackInfoReturnable<Boolean> cir)
    {
        if(oldItem.is(Items.LEATHER_HELMET))
        {
            removeTag("has_oxygen");
        }
        if(newItem.is(Items.LEATHER_HELMET))
        {
            addTag("has_oxygen");
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    private void checkKey(CallbackInfo ci)
    {
        AttributeInstance gravity = this.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if(this.level().dimension().location().getNamespace().equals("dataplanets"))
        {
            String name = this.level().dimension().location().getPath();
            CompoundTag data = StarSystemCreator.getDynamicDataOrNew();
            CompoundTag planetData = data.getCompound(name.substring(0,name.length()-1)).getCompound(name);
            if(gravity!=null)
            {
                float mult = (planetData.getFloat("gravity")/15f)-0.9f;

                AttributeModifier modifier = new AttributeModifier(Dataplanets.LOW_GRAVITY, "Low Gravity", mult, AttributeModifier.Operation.MULTIPLY_TOTAL);
                if(!gravity.hasModifier(modifier))
                {
                    gravity.addPermanentModifier(modifier);
                }
                else
                {
                    gravity.removeModifier(modifier);
                }


            }
            if(!level().isClientSide)
            {
                if(getOnPos().getY()>400 && !this.level().dimension().location().getPath().contains("orbit"))
                {
                    ResourceKey<LevelStem> resourcekey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",this.level().dimension().location().getPath()+"_orbit"));
                    if(this.getServer().levels.containsKey(resourcekey))
                    {
                        teleportTo(this.getServer().levels.get(resourcekey),getOnPos().getX(),100,getOnPos().getZ(),Set.of(),0,0);
                    }
                }
                if(getOnPos().getY()<10 && this.level().dimension().location().getPath().contains("orbit"))
                {
                    ResourceKey<LevelStem> resourcekey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets",this.level().dimension().location().getPath().replace("_orbit","")));
                    if(this.getServer().levels.containsKey(resourcekey))
                    {
                        teleportTo(this.getServer().levels.get(resourcekey),getOnPos().getX(),300,getOnPos().getZ(),Set.of(),0,0);
                    }
                }
                if(!planetData.getBoolean("hasAtmosphere") || !planetData.getBoolean("hasOxygen"))
                {
                    if(!getTags().contains("has_oxygen"))
                    {
                        addEffect(new MobEffectInstance(MobEffects.WITHER));
                    }
                }
                if(planetData.getInt("temperature")<263)
                {
                    if(!getTags().contains("has_heat"))
                    {
                        setTicksFrozen(200);
                    }
                }
                if(planetData.getInt("temperature")>313)
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
                gravity.removeModifier(Dataplanets.LOW_GRAVITY);
            }
        }
    }
}
