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
        if (mc.player != null && ModList.get().isLoaded("cosmos")) {
            mc.player.sendSystemMessage(
                    Component.literal("Both Cosmic Horizons and Genesis are installed. These are two different space mods and may cause unpredictable behavior when used together. Remove one of them before reporting any space-related bugs.")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
