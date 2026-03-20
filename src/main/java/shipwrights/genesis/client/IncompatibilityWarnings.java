package shipwrights.genesis.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class IncompatibilityWarnings {


    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ModList.get().isLoaded("oculus")) {
            mc.player.sendSystemMessage(
                    Component.literal("Oculus is incompatible with this version of Genesis. Visuals will be broken (even with shaders disabled).")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
