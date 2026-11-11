package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DerivedLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.space.Daylight;

/// {@code /time} by the sun that's drawn on a celestial (see {@link Daylight}), as it is by vanilla's: setting a time
/// puts the sun where vanilla's would be then, and the time reported is where vanilla's would be for where it is
@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {

    @WrapOperation(method = "setTime", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private static void genesis$setTimeBySun(ServerLevel level, long time, Operation<Void> original) {
        // Every other dimension keeps the overworld's time, ignoring its own being set
        long dayTime = level.getLevelData() instanceof DerivedLevelData ? time : Daylight.dayTimeFor(level, time);
        original.call(level, dayTime);
    }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private static void genesis$reportTimeBySun(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Daylight.vanillaDayTime(level, level.getDayTime()));
    }
}
