package shipwrights.dataplanets;

import net.minecraft.client.Minecraft;
import shipwrights.dataplanets.blocks.ResearchStationBE;
import shipwrights.dataplanets.screens.ResearchScreen;

public class ClientThings {


    public static void openResearchScreen(ResearchStationBE researchStationBE)
    {
        Minecraft.getInstance().setScreen(new ResearchScreen(researchStationBE));
    }
}
