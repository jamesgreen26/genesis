package shipwrights.genesis;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.StopVoidEngineStartSoundPacket;
import shipwrights.genesis.networking.VoidEngineSoundPacket;
import shipwrights.genesis.networking.WormholeTravelSoundPacket;
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
    private static final List<QueuedMoon> moonQueue = new CopyOnWriteArrayList<>();

    public GenesisMod(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();

        GenesisNetworking.INSTANCE.messageBuilder(WormholeTravelSoundPacket.class, 0)
                .encoder(WormholeTravelSoundPacket::encode)
                .decoder(WormholeTravelSoundPacket::decode)
                .consumerMainThread(WormholeTravelSoundPacket::handle)
                .add();

        GenesisNetworking.INSTANCE.messageBuilder(VoidEngineSoundPacket.class, 1)
                .encoder(VoidEngineSoundPacket::encode)
                .decoder(VoidEngineSoundPacket::decode)
                .consumerMainThread(VoidEngineSoundPacket::handle)
                .add();

        GenesisNetworking.INSTANCE.messageBuilder(StopVoidEngineStartSoundPacket.class, 2)
                .encoder(StopVoidEngineStartSoundPacket::encode)
                .decoder(StopVoidEngineStartSoundPacket::decode)
                .consumerMainThread(StopVoidEngineStartSoundPacket::handle)
                .add();

        GenesisBlocks.BLOCKS.register(eventBus);
        shipwrights.genesis.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.item.GenesisCreativeTabs.register(eventBus);
    }

    /// @param size relative to earth
    /// @param sunDist relative to earth
    public static void registerPlanet(ResourceLocation dimensionID, double size, double sunDist, int yearLengthTicks, float r, float g, float b) {
        for (PlanetData planet : planets) {
            if (planet.dimensionID.equals(dimensionID)) {
                return;
            }
        }

        if (sunDist * earthDist > 2048) {
            planets.add(new PlanetData(dimensionID, null, size, sunDist, yearLengthTicks, r, g, b));
        } else {
            LOGGER.warn("Failed to register planet {}, it is too close to the sun!", dimensionID);
        }
    }

    public static void registerMoon(ResourceLocation dimensionID, ResourceLocation orbitingDimensionID, double size, double orbitRadius, double yearLength, float r, float g, float b) {
        moonQueue.add(new QueuedMoon(dimensionID, orbitingDimensionID, size, orbitRadius, yearLength, r, g, b));
    }

    @ApiStatus.Internal
    public static void finalizeMoons() {
        for (var moon : moonQueue) {
            PlanetData parent = null;

            for (PlanetData planet : planets) {
                if (planet.dimensionID.equals(moon.dimensionID)) {
                    return;
                } else if (planet.dimensionID.equals(moon.orbitingDimensionID)) {
                    parent = planet;
                }
            }

            if (parent != null) {
                planets.add(new PlanetData(moon.dimensionID, parent, moon.size, moon.orbitRadius, (int)(moon.yearLength * earthYear), moon.r, moon.g, moon.b));
            } else {
                LOGGER.warn("Failed to register moon {}, its parent planet is missing!", moon.dimensionID);
            }
        }

        moonQueue.clear();
    }

    /// @param size relative to earth
    /// @param sunDist relative to earth
    /// @param yearLength relative to earth
    public static void registerPlanet(ResourceLocation dimensionID, double size, double sunDist, double yearLength, float r, float g, float b) {
        registerPlanet(dimensionID, size, sunDist, (int)(yearLength * earthYear), r, g, b);
    }

    public static void refreshEntityScaling(Entity entity, Level level) {
        try {
            ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
            ScaleData explosionScaleData = ScaleTypes.EXPLOSIONS.getScaleData(entity);
            scaleData.setPersistence(true);
            explosionScaleData.setPersistence(true);
            if (isMiniScale(level)) {
                scaleData.setScale(1 / 16f);
                explosionScaleData.setScale(16f);
                entity.setNoGravity(true);
            } else {
                scaleData.setScale(1f);
                explosionScaleData.setScale(1f);
                entity.setNoGravity(false);
            }
        } catch (Exception ignored) { /* not really sure what causes this, but I don't think it's critical */ }
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

    private record QueuedMoon(
            ResourceLocation dimensionID,
            ResourceLocation orbitingDimensionID,
            double size,
            double orbitRadius,
            double yearLength,
            float r,
            float g,
            float b
    ) {}
}
