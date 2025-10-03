package shipwrights.genesis.mixin.dataplanets;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.dataplanets.Dataplanets;

@Mixin(WorldOpenFlows.class)
public class WorldSelectionMixin {


    @Inject(method = "doLoadLevel*", at = @At("HEAD"), remap = false)
    private void load(Screen arg, String string, boolean bl, boolean bl2, boolean confirmExperimentalWarning, CallbackInfo ci)
    {
        System.out.println("LOADING LEVEL!");
        Dataplanets.LAST_WORLD_ID = string;
    }
}
