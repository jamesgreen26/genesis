package g_mungus.genesis.mixin;

import g_mungus.genesis.GenesisMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow public abstract ServerLevel getLevel();

    @Unique
    private boolean genesis$isSpace() {
        return getLevel().dimension().location().equals(GenesisMod.SPACE_DIM);
    }

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void addEntityMixin(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        GenesisMod.refreshEntityScaling(entity, genesis$isSpace());
    }

    @Inject(method = "addPlayer", at = @At("HEAD"))
    private void addPlayerMixin(ServerPlayer arg, CallbackInfo ci) {
        GenesisMod.refreshEntityScaling(arg, genesis$isSpace());
    }
}
