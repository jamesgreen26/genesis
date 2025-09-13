package shipwrights.genesis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.client.DimensionEffects;
import shipwrights.genesis.worldgen.AsteroidBelt;
import shipwrights.genesis.worldgen.AsteroidBlockSurfaceRule;
import shipwrights.genesis.worldgen.RandomNoise;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

@Mod.EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";

    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");

    public static final int atmosphereCollisionHeight = 2048;

    public GenesisMod() {}

    @SubscribeEvent
    public static void onLevelStartup(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            if (dimension.location().equals(SPACE_DIM)) {
                // this works, just needs a newer VS core version than is released
                // VSGameUtilsKt.getShipObjectWorld(serverLevel).updateDimension(dimension.registry() + ":" + dimension.location(), new Vector3d());
            }
        }


    }

    public static void refreshEntityScaling(Entity entity, Boolean miniScale) {
        ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
        scaleData.setPersistence(true);
        if (miniScale) {
            scaleData.setScale(1 / 16f);
            entity.setNoGravity(true);
        } else {
            scaleData.setScale(1f);
            entity.setNoGravity(false);
        }
    }
}
