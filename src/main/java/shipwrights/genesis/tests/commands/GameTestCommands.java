package shipwrights.genesis.tests.commands;

import net.minecraftforge.event.RegisterCommandsEvent;

public class GameTestCommands {

    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        AssembleShipCommand.register(event);
    }
}
