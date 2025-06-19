package g_mungus.genesis;

import g_mungus.genesis.space.GreatUnknownDimension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

@EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";

    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final int atmosphereCollisionHeight = 2048;

    public GenesisMod(IEventBus eventBus) {
        eventBus.addListener(GreatUnknownDimension::registerEffects);

        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("overworld"), 24, new Vector3i(2048, -64, 32), new Vector3f(30, 20, 10));
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

        ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
        scaleData.setPersistence(true);
        if (miniScale) {
            scaleData.setScale(1 / 16f);
        } else {
            scaleData.setScale(1f);
        }
    }
}
