package shipwrights.genesis;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.entity.handling.DefaultShipyardEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import shipwrights.genesis.commands.GenesisCommandArguments;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.time.GenesisTimeData;
import shipwrights.genesis.time.TimeTracker;
import shipwrights.genesis.config.GenesisClientConfig;
import shipwrights.genesis.content.block.GenesisBlocks;
import shipwrights.genesis.content.fluid.GenesisFluids;
import shipwrights.genesis.content.particle.GenesisParticles;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.transformProvider.BuiltinTransformProviders;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.teleportation.impl.ShipCollector;
import shipwrights.genesis.teleportation.integration.PlanetToSpaceTeleporter;
import shipwrights.genesis.teleportation.integration.SpaceToPlanetTeleporter;
import shipwrights.genesis.tests.commands.GameTestCommands;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.regex.Pattern;

@Mod.EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);
    public static final Vector3dc EAST = new Vector3d(1.0, 0.0, 0.0);

    public static long clientTimeOffset = 0;

    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation WORMHOLE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "subspace");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");
    public static ResourceLocation GENERIC_PLANET_ID = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "planet");

    public static final ResourceKey<Registry<Celestial>> CELESTIALS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "celestials"));

    private static final Pattern SEAT_REGISTRY_NAME =
            Pattern.compile("(?<![a-z])(seat|chair)(?![a-z])", Pattern.CASE_INSENSITIVE);


    public GenesisMod(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.CLIENT, GenesisClientConfig.CONFIG_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, GenesisCommonConfig.CONFIG_SPEC);

        // Register the celestials datapack registry
        eventBus.addListener(GenesisMod::registerDataPackRegistries);

        // Register packet handlers
        GenesisNetworking.init();

        // Register celestial types
        BuiltinCelestialTypes.register();

        // Register celestial transform providers
        BuiltinTransformProviders.register();

        // Register fluids using Registrate (must be called before other registrations)
        GenesisFluids.init();

        GenesisCommandArguments.register(eventBus);

        GenesisBlocks.BLOCKS.register(eventBus);
        GenesisBlocks.MENU_TYPES.register(eventBus);
        GenesisParticles.PARTICLE_TYPES.register(eventBus);

        shipwrights.genesis.content.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.content.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.content.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.content.item.GenesisCreativeTabs.register(eventBus);
        shipwrights.genesis.content.painting.GenesisPaintings.PAINTING_VARIANTS.register(eventBus);

        ValkyrienSkies.api().getPhysTickEvent().on(ShipCollector::onPhysTick);

        boolean isGameTest = System.getProperty("forge.enabledGameTestNamespaces") != null;
        MinecraftForge.EVENT_BUS.register(new PlanetToSpaceTeleporter(isGameTest));
        MinecraftForge.EVENT_BUS.register(new SpaceToPlanetTeleporter(isGameTest));
        MinecraftForge.EVENT_BUS.register(TimeTracker.class);

        if (isGameTest) {
            MinecraftForge.EVENT_BUS.addListener(GameTestCommands::onRegisterCommandsEvent);
        }
    }

    private static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CELESTIALS_KEY, Celestial.CODEC, Celestial.CODEC);
    }

    public static Registry<Celestial> getCelestialRegistry(Level level) {
        return level.registryAccess().registryOrThrow(CELESTIALS_KEY);
    }

    @Nullable public static Celestial getCelestialForLevel(Level level) {
        return getCelestialRegistry(level).get(level.dimension().location());
    }

    @SuppressWarnings("ConstantConditions")
    public static long getTicks(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (server == null || server.overworld() == null) return level.getGameTime();
            return level.getGameTime() + GenesisTimeData.getOrCreate(server).getTimeOffset();
        }
        return level.getGameTime() + clientTimeOffset;
    }

    public static float getPartialTick(Level level, RenderLevelStageEvent event) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return 0f;
        return event.getPartialTick();
    }

    @Deprecated
    public static boolean isMiniScale(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    @Deprecated
    public static boolean isMiniScale(Level level) {
        return isMiniScale(level.dimension().location());
    }

    public static double getDimensionScale(Level level) {
        if (level == null) return 1.0;
        ResourceLocation dimension = level.dimension().location();
        if (dimension.equals(WORMHOLE_DIM) || dimension.equals(SPACE_DIM)) {
            return 1.0 / 16.0;
        } else {
            return 1.0;
        }
    }

    public static boolean shouldCancelVoidDamage(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    public static boolean shouldCancelVoidDamage(Level level) {
        return shouldCancelVoidDamage(level.dimension().location());
    }

    public static boolean isSpaceDimension(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM);
    }

    public static boolean isSpaceDimension(Level level) {
        return isSpaceDimension(level.dimension().location());
    }

    public static boolean isSubSpaceDimension(Level level) {
        return level.dimension().location().equals(WORMHOLE_DIM);
    }

    public static double getApparentSunAngle(double starUpDot, double starEastDot) {
        boolean sign = Math.signum(starUpDot) < 0;
        double starDot0To1 = (1 - starEastDot) / 4;
        return sign ? 1 - starDot0To1 : starDot0To1;
    }

    @ApiStatus.Internal
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
}
