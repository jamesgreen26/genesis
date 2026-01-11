package shipwrights.genesis;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.entity.handling.DefaultShipyardEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import shipwrights.genesis.content.block.GenesisBlocks;
import shipwrights.genesis.content.fluid.GenesisFluids;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.space.ExampleCustomTransformProvider;
import shipwrights.genesis.content.particle.GenesisParticles;
import shipwrights.genesis.teleportation.ShipLandingAttachment;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.registry.SpaceRegistry;
import shipwrights.genesis.teleportation.TeleportationHandler;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
@Mod(GenesisMod.MOD_ID)
public final class GenesisMod {
    public static final String MOD_ID = "genesis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "great_unknown");
    public static final ResourceLocation WORMHOLE_DIM = ResourceLocation.fromNamespaceAndPath(MOD_ID, "wormhole");
    public static final ResourceLocation ASTEROID_RULE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "asteroid_block_surface_rule");
    public static ResourceLocation ORBITING_ID = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "orbiting");

    private static final Pattern SEAT_REGISTRY_NAME =
            Pattern.compile("(?<![a-z])(seat|chair)(?![a-z])", Pattern.CASE_INSENSITIVE);

    public static final int atmosphereExitHeight = 2048;
    public static final int atmosphereEntryHeight = 1440;

    private static final List<Consumer<SpaceRegistry.RegisterCelestialsEvent>> registrationCallbacks = new CopyOnWriteArrayList<>();
    public static final SpaceRegistry SPACE_REGISTRY = new SpaceRegistry(registrationCallbacks);

    public GenesisMod(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();

        // Register packet handlers
        GenesisNetworking.init();

        // Register custom transform providers
        ExampleCustomTransformProvider.register();

        // Register fluids using Registrate (must be called before other registrations)
        GenesisFluids.init();

        GenesisBlocks.BLOCKS.register(eventBus);
        GenesisBlocks.MENU_TYPES.register(eventBus);
        GenesisParticles.PARTICLE_TYPES.register(eventBus);

        shipwrights.genesis.content.blockentity.GenesisBlockEntities.BLOCK_ENTITIES.register(eventBus);
        shipwrights.genesis.content.sound.GenesisSounds.SOUND_EVENTS.register(eventBus);
        shipwrights.genesis.content.item.GenesisItems.ITEMS.register(eventBus);
        shipwrights.genesis.content.item.GenesisCreativeTabs.register(eventBus);
        shipwrights.genesis.content.painting.GenesisPaintings.PAINTING_VARIANTS.register(eventBus);

        ValkyrienSkies.api().registerAttachment(ShipLandingAttachment.class);

        ValkyrienSkies.api().getPhysTickEvent().on(TeleportationHandler::onPhysTick);
    }

    public static void onRegisterCelestialsEvent(Consumer<SpaceRegistry.RegisterCelestialsEvent> callback) {
        registrationCallbacks.add(callback);
    }

    @Nullable public static OrbitingBody getDataForLevel(Level level) {
        return SPACE_REGISTRY.getOrbitingBody(level.dimension().location());
    }

    public static boolean isMiniScale(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(SPACE_DIM) || dimensionLocation.equals(WORMHOLE_DIM);
    }

    public static boolean isMiniScale(Level level) {
        return isMiniScale(level.dimension().location());
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
