package shipwrights.dataplanets;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.dataplanets.systemCreation.naming.FantasySystemNameGenerator;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.util.RegistryUtil;

@Mod.EventBusSubscriber
public class DataplanetsMod {
    public static final String MOD_ID = "dataplanets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final FantasySystemNameGenerator FANTASY_SYSTEM_NAME_GENERATOR = new FantasySystemNameGenerator();

    @SubscribeEvent
    public static void onDataReload(AddReloadListenerEvent event) {
        event.addListener(FANTASY_SYSTEM_NAME_GENERATOR);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();

        boolean isNewSave = RegistryUtil.setupDatapackFolder(server);

        if (isNewSave) {
            new SystemCreator().createSystem(server, true);
        }
    }
}
