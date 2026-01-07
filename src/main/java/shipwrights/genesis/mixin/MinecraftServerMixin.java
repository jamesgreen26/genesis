package shipwrights.genesis.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;

import java.util.Map;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Final
    @Shadow
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Inject(method = "prepareLevels", at = @At("RETURN"))
    public void onLevelsCreated(ChunkProgressListener arg, CallbackInfo ci) {
        for (ServerLevel level : levels.values()) {
            if (GenesisMod.isMiniScale(level)) {
                VSGameUtilsKt.getShipObjectWorld(level).updateDimension(
                        VSGameUtilsKt.getDimensionId(level), new Vector3d(), 63d, -1d
                );
            }
        }
    }
}
