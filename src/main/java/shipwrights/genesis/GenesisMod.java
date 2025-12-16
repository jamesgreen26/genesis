package shipwrights.genesis;

import g_mungus.vlib.data.DimensionSettings;
import g_mungus.vlib.dimension.DimensionSettingsManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.entity.handling.DefaultShipyardEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import shipwrights.genesis.fluid.GenesisFluids;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.StopVoidEngineStartSoundPacket;
import shipwrights.genesis.networking.VoidEngineSoundPacket;
import shipwrights.genesis.networking.WormholeTravelSoundPacket;
import shipwrights.genesis.planets.PlanetData;
import shipwrights.genesis.ship.ShipLandingAttachment;
import shipwrights.genesis.teleportation.TeleportationHandler;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation WORMHOLE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "wormhole");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");

    private static final Pattern SEAT_REGISTRY_NAME =
            Pattern.compile("(?<![a-z])(seat|chair)(?![a-z])", Pattern.CASE_INSENSITIVE);

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

        // Register fluids using Registrate (must be called before other registrations)
        GenesisFluids.init();

        GenesisBlocks.BLOCKS.register(eventBus);
        GenesisBlocks.MENU_TYPES.register(eventBus);
        shipwrights.genesis.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.item.GenesisCreativeTabs.register(eventBus);
        shipwrights.genesis.painting.GenesisPaintings.PAINTING_VARIANTS.register(eventBus);

        ValkyrienSkies.api().registerAttachment(ShipLandingAttachment.class);

        ValkyrienSkies.api().getPhysTickEvent().on(TeleportationHandler::onPhysTick);
    }

    /// @param size       relative to earth
    /// @param sunDist    relative to earth
    /// @param yearLength relative to earth
    /// @param gravity    relative to earth
    ///
    /// @apiNote Registered planets get reset each time the server stops
    /// pineapple
    public static Optional<PlanetData> registerPlanet(ResourceLocation dimensionID, double size, double gravity, double sunDist, double yearLength, float r, float g, float b) {
        for (PlanetData planet : planets) {
            if (planet.dimensionID.equals(dimensionID)) {
                return Optional.empty();
            }
        }

        if (sunDist * earthDist > 2048) {
            PlanetData data = new PlanetData(dimensionID, null, size, sunDist, yearLength, r, g, b, gravity);
            planets.add(data);
            DimensionSettingsManager.INSTANCE.addSettings(data.dimensionID, new DimensionSettings(1.0, gravity, true));

            return Optional.of(data);
        } else {
            LOGGER.warn("Failed to register planet {}, it is too close to the sun!", dimensionID);
            return Optional.empty();
        }
    }

    public static void registerMoon(ResourceLocation dimensionID, ResourceLocation orbitingDimensionID, double size, double gravity, double orbitRadius, double yearLength, float r, float g, float b) {
        DimensionSettingsManager.INSTANCE.addSettings(dimensionID, new DimensionSettings(1.0, gravity, true));

        moonQueue.add(new QueuedMoon(dimensionID, orbitingDimensionID, size, gravity, orbitRadius, yearLength, r, g, b));
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
                planets.add(new PlanetData(moon.dimensionID, parent, moon.size, moon.orbitRadius, moon.yearLength, moon.r, moon.g, moon.b, moon.gravity));
            } else {
                LOGGER.warn("Failed to register moon {}, its parent planet is missing!", moon.dimensionID);
            }
        }

        moonQueue.clear();
    }

    public static void refreshEntityScaling(Entity entity, Level level) {
        try {
            ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
            ScaleData explosionScaleData = ScaleTypes.EXPLOSIONS.getScaleData(entity);
            scaleData.setPersistence(true);
            explosionScaleData.setPersistence(true);
            if (isMiniScale(level)) {
                ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (
                        entity instanceof Projectile ||
                        VSEntityManager.INSTANCE.getHandler(entity) != DefaultShipyardEntityHandler.INSTANCE ||
                        SEAT_REGISTRY_NAME.matcher(entityType.getPath()).find()
                ) {
                    scaleData.setScale(1 / 16f);
                    explosionScaleData.setScale(16f);
                }
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
            double gravity,
            double orbitRadius,
            double yearLength,
            float r,
            float g,
            float b
    ) {}
}
