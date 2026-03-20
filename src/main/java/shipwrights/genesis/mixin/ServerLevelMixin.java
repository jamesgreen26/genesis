package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow public abstract ServerLevel getLevel();

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void addEntityMixin(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        GenesisMod.refreshEntityScaling(entity, getLevel());
    }

    @Inject(method = "addPlayer", at = @At("HEAD"))
    private void addPlayerMixin(ServerPlayer arg, CallbackInfo ci) {
        GenesisMod.refreshEntityScaling(arg, getLevel());
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean genesis$shouldSnow(Biome instance, LevelReader levelReader, BlockPos pos, Operation<Boolean> original) {
        ServerLevel level = getLevel();
        if (GenesisMod.shouldCancelVoidDamage(level)) {
            return false;
        }
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);

        if (ship != null && ship.getTransform().getPositionInWorld().y() > 400) {
            return false;
        }

        return original.call(instance, level, pos);
    }
}
