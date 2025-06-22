package g_mungus.genesis;

import g_mungus.genesis.asteroid.AsteroidBlock;
import g_mungus.genesis.item.TestingStickItem;
import g_mungus.genesis.client.GreatUnknownDimension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MOD_ID);

    public static final DeferredHolder<Item, TestingStickItem> TESTING_STICK_ITEM = ITEMS.register("testing_stick",
            () -> new TestingStickItem(new Item.Properties()));

    public static final DeferredHolder<Block, AsteroidBlock> ASTEROID_BLOCK = BLOCKS.register("asteroid_block",
            () -> new AsteroidBlock(BlockBehaviour.Properties.of()));

    public GenesisMod(IEventBus eventBus) {
        eventBus.addListener(GreatUnknownDimension::registerEffects);

        ITEMS.register(eventBus);
        BLOCKS.register(eventBus);
        registerPlanets();
    }

    private static void registerPlanets() {
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("overworld"), 256, new Vector3i(50000, -64, 32), new Vector3f(30, 20, 10));

        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("a"), 144, new Vector3i(20000, -32800, -500), new Vector3f(0, 70, 0));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("b"), 108, new Vector3i(-12500, 10000, 3750), new Vector3f(30, 45, 10));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("c"), 180, new Vector3i(30000, -20000, 6250), new Vector3f(0, 90, 0));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("d"), 132, new Vector3i(8000, 15500, -11250), new Vector3f(15, 30, 45));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("e"), 96, new Vector3i(-17500, -7500, 1750), new Vector3f(60, 0, 20));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("f"), 156, new Vector3i(25000, 25000, 25000), new Vector3f(0, 180, 0));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("g"), 120, new Vector3i(-30000, 5000, -15000), new Vector3f(90, 45, 0));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("h"), 168, new Vector3i(10000, -10000, 5000), new Vector3f(0, 135, 15));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("i"), 114, new Vector3i(30000, 0, 23000), new Vector3f(10, 10, 10));
        PlanetRegistry.registerPlanet(ResourceLocation.withDefaultNamespace("j"), 150, new Vector3i(16000, -23000, 7750), new Vector3f(5, 60, 30));
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
