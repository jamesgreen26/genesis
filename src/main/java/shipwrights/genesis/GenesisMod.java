package shipwrights.genesis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    public static final int earthYear = 4608000;

    public static final List<PlanetData> planets = new CopyOnWriteArrayList<>();

    public GenesisMod(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();;

        GenesisBlocks.BLOCKS.register(eventBus);
        shipwrights.genesis.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.item.GenesisCreativeTabs.register(eventBus);

        registerPlanet(ResourceLocation.parse("minecraft:overworld"), 1.0, 1.0, earthYear, 0, 0.5f, 0.8f);

        for (var planet: planets) {
            LOGGER.warn(planet.toString());
        }

    }

    /// @param size relative to earth
    /// @param sunDist relative to earth
    public static void registerPlanet(ResourceLocation dimensionID, double size, double sunDist, int yearLength, float r, float g, float b) {
        for (PlanetData planet : planets) {
            if (planet.dimensionID.equals(dimensionID)) {
                return; //fixme
            }
        }

        if (sunDist * earthDist > 2048) {
            planets.add(new PlanetData(dimensionID, size, sunDist, yearLength, r, g, b));
        } else {
            LOGGER.warn("Failed to register planet {}, it is too close to the sun!", dimensionID);
        }
    }

    @SubscribeEvent
    public static void modifyFov(ViewportEvent.ComputeFov event) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (
            player != null &&
            isSpaceDimension(player.level()) &&
            !Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
            player.isPassenger()
        ) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && VSEntityManager.INSTANCE.getHandler(vehicle) == DefaultShipyardEntityHandler.INSTANCE) {
                event.setFOV(event.getFOV() * 0.25);
            }
        }
    }

    public static void refreshEntityScaling(Entity entity, Level level) {
        ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
        scaleData.setPersistence(true);
        if (isMiniScale(level)) {
            scaleData.setScale(1 / 16f);
            entity.setNoGravity(true);
        } else {
            scaleData.setScale(1f);
            entity.setNoGravity(false);
        }
    }

    public static boolean isMiniScale(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    public static boolean isMiniScale(ResourceKey<Level> dimension) {
        return isMiniScale(dimension.location());
    }

    public static boolean isMiniScale(Level level) {
        return isMiniScale(level.dimension().location());
    }

    public static boolean shouldCancelVoidDamage(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    public static boolean shouldCancelVoidDamage(ResourceKey<Level> dimension) {
        return shouldCancelVoidDamage(dimension.location());
    }

    public static boolean shouldCancelVoidDamage(Level level) {
        return shouldCancelVoidDamage(level.dimension().location());
    }

    public static boolean isSpaceDimension(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM);
    }

    public static boolean isSpaceDimension(ResourceKey<Level> dimension) {
        return isSpaceDimension(dimension.location());
    }

    public static boolean isSpaceDimension(Level level) {
        return isSpaceDimension(level.dimension().location());
    }
}
