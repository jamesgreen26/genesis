package shipwrights.genesis.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.ClientStorage;
import shipwrights.genesis.client.WarpLoadingMenu;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    static Minecraft instance;

    @Shadow
    public Screen screen;

    /**
     * Note: this is not up to date on actual dimension changes.
     * We only update this from the dimension transfer screen, and
     * only use it for the dimension transfer screen
     */
    @Unique
    private ResourceLocation previousDim;

    // This is called twice.
    // Once on leaving the current level
    // And once when in the new level
    @Inject(
            method = "setScreen",
            at = @At("RETURN")
    )
    private void injectSetScreen(Screen s, CallbackInfo ci) {
        // Prevent recursion
        if (s instanceof WarpLoadingMenu) return;

        if (s instanceof ReceivingLevelScreen || s instanceof ProgressScreen) {
            if (instance.level == null) return;

            if (ClientStorage.goingToFromWormhole) {
                instance.setScreen(new WarpLoadingMenu());
            }
        }
    }

}
