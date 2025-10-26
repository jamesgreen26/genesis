package shipwrights.dataplanets;

import com.tterrag.registrate.Registrate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import shipwrights.dataplanets.registry.DPBlocks;
import shipwrights.dataplanets.registry.DPEntities;
import shipwrights.dataplanets.registry.DPItems;
import shipwrights.dataplanets.systemCreation.naming.FantasySystemNameGenerator;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;
import shipwrights.genesis.GenesisMod;

import java.util.List;

@Mod.EventBusSubscriber
@Mod(DataplanetsMod.MOD_ID)
public class DataplanetsMod {
    public static final String MOD_ID = "dataplanets";
    public static final Logger LOGGER = GenesisMod.LOGGER;

    public static final FantasySystemNameGenerator FANTASY_SYSTEM_NAME_GENERATOR = new FantasySystemNameGenerator();

    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public static final ResourceLocation MUTABLE_DATA = ResourceLocation.fromNamespaceAndPath("dataplanets","mutable_data");

    public DataplanetsMod(FMLJavaModLoadingContext context) {
        DPItems.init();
        DPBlocks.init();
        DPEntities.init();
    }


    @SubscribeEvent
    public static void onDataReload(AddReloadListenerEvent event) {
        event.addListener(FANTASY_SYSTEM_NAME_GENERATOR);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();

        boolean isNewSave = RegistryUtil.setupDatapackFolder(server);

        if (isNewSave) {
            List<String> rawNames = new SystemCreator().createSystem(server, true);

            CompoundTag tag = server.getCommandStorage().get(MUTABLE_DATA);
            CompoundTag system = new CompoundTag();
            for (int i = 1; i < rawNames.size(); i++) {
                CompoundTag planet = new CompoundTag();
                //TODO: faction data should exist here
                system.put(rawNames.get(i),planet);
            }
            tag.put(rawNames.get(0),system);
            server.getCommandStorage().set(MUTABLE_DATA,tag);
        }
    }
}
