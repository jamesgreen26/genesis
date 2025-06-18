package g_mungus.genesis;

import g_mungus.genesis.dimension.GreatUnknownDimension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";

    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");

    private static final ResourceLocation entityScale = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "entity_scale");


    public GenesisMod(IEventBus eventBus) {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like registries and resources) may still be uninitialized.
        // Proceed with mild caution.
        eventBus.addListener(GreatUnknownDimension::registerEffects);
    }

    @SubscribeEvent
    public static void onLevelStartup(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (dimension.location().equals(SPACE_DIM))
                VSGameUtilsKt.getShipObjectWorld(serverLevel).updateDimension(dimension.registry() + ":" + dimension.location(), new Vector3d());
        }
    }

    public static void refreshEntityScaling(Entity entity, Boolean miniScale) {
        if (entity instanceof LivingEntity livingEntity) {
            AttributeInstance scale = livingEntity.getAttributes().getInstance(Attributes.SCALE);
            if (miniScale) {
                if (scale != null) {
                    AttributeModifier attributeModifier = new AttributeModifier(entityScale, -15.0 / 16.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                    scale.addOrReplacePermanentModifier(attributeModifier);
                }
            } else {
                if (scale != null) {
                    scale.removeModifier(entityScale);
                }
            }
        }
    }
}
