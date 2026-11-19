package shipwrights.genesis.space.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import shipwrights.genesis.GenesisMod;

/// Adds a button to the title screen, left of the language button and showing a spyglass, that opens the
/// {@link EditorProjectsScreen}, and from there the {@link SolarSystemScreen}. The title screen is the only way to
/// them.
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class SolarSystemMapButton {
    private static final Component MESSAGE = Component.translatable("menu.genesis.solar_system");

    private SolarSystemMapButton() {
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;

        // Beside the language button, wherever the title screen and other mods have put it
        SpriteIconButton language = event.getListenersList().stream()
                .filter(listener -> listener instanceof SpriteIconButton button
                        && button.getMessage().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("options.language"))
                .map(SpriteIconButton.class::cast)
                .findFirst()
                .orElse(null);
        if (language == null) return;

        event.addListener(new ItemIconButton(language.getX() - 24, language.getY(), 20, new ItemStack(Items.SPYGLASS),
                MESSAGE, pressed -> Minecraft.getInstance().setScreen(new EditorProjectsScreen(screen))));
    }
}
