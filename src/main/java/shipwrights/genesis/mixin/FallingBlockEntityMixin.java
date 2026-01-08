package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.mixin_extension.FallingBlockEntityExtension;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin implements FallingBlockEntityExtension {

    @Unique
    private static final EntityDataAccessor<Float> GENESIS_ROT_X = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> GENESIS_ROT_Y = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> GENESIS_ROT_Z = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.FLOAT);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void defineRotationData(CallbackInfo ci) {
        ((FallingBlockEntity)(Object)this).getEntityData().define(GENESIS_ROT_X, 0.0f);
        ((FallingBlockEntity)(Object)this).getEntityData().define(GENESIS_ROT_Y, 0.0f);
        ((FallingBlockEntity)(Object)this).getEntityData().define(GENESIS_ROT_Z, 0.0f);
    }

    @Override
    public void genesis$setRotation(Vector3d rotation) {
        FallingBlockEntity entity = (FallingBlockEntity)(Object)this;
        entity.getEntityData().set(GENESIS_ROT_X, (float) rotation.x);
        entity.getEntityData().set(GENESIS_ROT_Y, (float) rotation.y);
        entity.getEntityData().set(GENESIS_ROT_Z, (float) rotation.z);
    }

    @Override
    public Vector3d genesis$getRotation() {
        FallingBlockEntity entity = (FallingBlockEntity)(Object)this;
        return new Vector3d(
                entity.getEntityData().get(GENESIS_ROT_X),
                entity.getEntityData().get(GENESIS_ROT_Y),
                entity.getEntityData().get(GENESIS_ROT_Z)
        );
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveRotation(CompoundTag compound, CallbackInfo ci) {
        Vector3d rotation = genesis$getRotation();
        compound.putDouble("genesis_rotX", rotation.x);
        compound.putDouble("genesis_rotY", rotation.y);
        compound.putDouble("genesis_rotZ", rotation.z);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadRotation(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("genesis_rotX")) {
            genesis$setRotation(new Vector3d(
                    compound.getDouble("genesis_rotX"),
                    compound.getDouble("genesis_rotY"),
                    compound.getDouble("genesis_rotZ")
            ));
        }
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V"
            )
    )
    private void genesis$preventEarlyDespawn(FallingBlockEntity entity, Operation<Void> original) {
        if (entity.time >= 600 || !GenesisMod.shouldCancelVoidDamage(entity.level()) || entity.getBlockState().isAir()) {
            original.call(entity);
        }
    }
}
