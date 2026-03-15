package shipwrights.genesis.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.commands.CelestialArgument;

@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosMixin {

    @Shadow
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> register(
            Registry<ArgumentTypeInfo<?, ?>> registry,
            String id,
            Class<? extends A> clazz,
            ArgumentTypeInfo<A, T> info
    ) {
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void genesis$bootstrap(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
        register(
                registry,
                "genesis:celestial",
                CelestialArgument.class,
                SingletonArgumentInfo.contextFree(CelestialArgument::celestial)
        );
    }
}
