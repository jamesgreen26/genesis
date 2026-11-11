package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.space.client.CelestialApproachOverlay;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /// Opening a screen frees the mouse, which would show the cursor and stop the camera turning while the screens
    /// standing in for vanilla's loading screens are open, as travel changes dimension
    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"))
    private boolean genesis$keepMouseForInvisibleScreens(MouseHandler mouseHandler,
                                                         @Local(argsOnly = true) Screen screen) {
        return !(screen instanceof CelestialApproachOverlay.KeepsInput);
    }

    /// Opening a screen lets go of every key, which would stop what's held, like moving, while those screens are open
    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;releaseAll()V"))
    private boolean genesis$keepKeysForInvisibleScreens(@Local(argsOnly = true) Screen screen) {
        return !(screen instanceof CelestialApproachOverlay.KeepsInput);
    }
}
