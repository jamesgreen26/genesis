package shipwrights.genesis.mixin.dataplanets;

import shipwrights.dataplanets.space.DynamicSystems;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
    @Shadow @Final private Map<String, String> storage;

    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void translate(String p_118920_, String p_265273_, CallbackInfoReturnable<String> cir)
    {
        if(!this.storage.containsKey(p_118920_))
        {
            cir.setReturnValue(DynamicSystems.TRANSLATIONS.getOrDefault(p_118920_,p_265273_));
        }
    }
}
