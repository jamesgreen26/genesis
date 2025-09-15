package shipwrights.genesis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.valkyrienskies.mod.common.entity.handling.DefaultShipyardEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
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
    public static void modifyFov(ViewportEvent.ComputeFov event) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (
            player != null &&
            player.level().dimension().location().equals(GenesisMod.SPACE_DIM) &&
            !Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
            player.isPassenger()
        ) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && VSEntityManager.INSTANCE.getHandler(vehicle) == DefaultShipyardEntityHandler.INSTANCE) {
                event.setFOV(event.getFOV() * 0.25);
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
