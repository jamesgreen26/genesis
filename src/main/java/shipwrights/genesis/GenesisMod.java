package shipwrights.genesis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.mod.common.entity.handling.DefaultShipyardEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import shipwrights.genesis.planets.PlanetData;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation WORMHOLE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "wormhole");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");

    public static final int atmosphereExitHeight = 2048;
    public static final int atmosphereEntryHeight = 1440;

    public static final double earthDist = 15_000;
    public static final double earthSize = 96;

    public static final List<PlanetData> planets = new ArrayList<>();

    public GenesisMod(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();;

        GenesisBlocks.BLOCKS.register(eventBus);
        shipwrights.genesis.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.item.GenesisCreativeTabs.register(eventBus);

        registerPlanet(ResourceLocation.parse("minecraft:overworld"), 1.0, 1.0, 0, 0.5f, 0.8f);

        for (var planet: planets) {
            LOGGER.warn(planet.toString());
        }

    }

    /// @param size relative to earth
    /// @param sunDist relative to earth
    public static void registerPlanet(ResourceLocation dimensionID, double size, double sunDist, float r, float g, float b) {
        for (PlanetData planet : planets) {
            if (planet.dimensionID.equals(dimensionID)) {
                LOGGER.warn("Failed to register planet {}, a planet with this dimension ID already exists!", dimensionID);
                return;
            }
        }

        if (sunDist * earthDist > 2048) {
            planets.add(new PlanetData(dimensionID, size, sunDist, r, g, b));
        } else {
            LOGGER.warn("Failed to register planet {}, it is too close to the sun!", dimensionID);
        }
    }

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
