package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Final
    @Shadow
    private ResourceKey<Level> dimension;

    @WrapMethod(method = "getDayTime")
    public long getDayTimeWrap(Operation<Long> original) {
        Level thisAsLevel = (Level)(Object)this;
        Celestial body = GenesisMod.getDataForLevel(thisAsLevel);

        if (body != null) {
            return body.getDayTime(GenesisMod.getTicks(thisAsLevel));
        }
        return original.call();
    }
}
