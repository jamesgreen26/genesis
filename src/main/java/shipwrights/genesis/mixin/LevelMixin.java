package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

@Mixin(Level.class)
public abstract class LevelMixin {

    @WrapMethod(method = "getDayTime")
    public long getDayTimeWrap(Operation<Long> original) {
        Level thisAsLevel = (Level)(Object)this;
        long gameTime = GenesisMod.getTicks(thisAsLevel);
        VantagePoint vp = VantagePoint.get(thisAsLevel, new Vector3d(), gameTime, 0f);

        if (vp instanceof VantagePoint.OnCelestial oc) {
            return (long)(genesis$getApparentAngle(oc, gameTime, 0f) * 24000);
        }
        return original.call();
    }

    @WrapMethod(method = "getSunAngle")
    public float getSunAngleWrap(float partialTick, Operation<Float> original) {
        Level thisAsLevel = (Level)(Object)this;
        long gameTime = GenesisMod.getTicks(thisAsLevel);
        VantagePoint vp = VantagePoint.get(thisAsLevel, new Vector3d(), gameTime, partialTick);

        if (vp instanceof VantagePoint.OnCelestial oc) {
            return (float)(genesis$getApparentAngle(oc, gameTime, partialTick) * Mth.TWO_PI);
        }
        return original.call(partialTick);
    }

    @WrapMethod(method = "isDay")
    public boolean isDayWrap(Operation<Boolean> original) {
        Level thisAsLevel = (Level)(Object)this;
        long gameTime = GenesisMod.getTicks(thisAsLevel);
        VantagePoint vp = VantagePoint.get(thisAsLevel, new Vector3d(), gameTime, 0f);

        if (vp instanceof VantagePoint.OnCelestial oc) {
            return genesis$getStarUpDot(oc, gameTime) >= 0;
        }
        return original.call();
    }

    @WrapMethod(method = "isNight")
    public boolean isNightWrap(Operation<Boolean> original) {
        Level thisAsLevel = (Level)(Object)this;
        long gameTime = GenesisMod.getTicks(thisAsLevel);
        VantagePoint vp = VantagePoint.get(thisAsLevel, new Vector3d(), gameTime, 0f);

        if (vp instanceof VantagePoint.OnCelestial oc) {
            return genesis$getStarUpDot(oc, gameTime) < 0;
        }
        return original.call();
    }

    @Unique
    private static double genesis$getApparentAngle(VantagePoint.OnCelestial oc, long gameTime, float partialTick) {
        Celestial celestial = oc.celestial();
        Celestial star = celestial.getNearestStar(gameTime, partialTick, oc.registry());

        Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick, oc.registry()))
                .sub(celestial.getPosition(gameTime, partialTick, oc.registry()))
                .normalize();

        Quaterniondc rot = new Quaterniond(oc.getRotation()).conjugate();
        toStar.rotate(rot);

        return GenesisMod.getApparentSunAngle(
                GenesisMod.UP.dot(toStar),
                GenesisMod.EAST.dot(toStar));
    }

    @Unique
    private static double genesis$getStarUpDot(VantagePoint.OnCelestial oc, long gameTime) {
        Celestial celestial = oc.celestial();
        Celestial star = celestial.getNearestStar(gameTime, 0f, oc.registry());

        Vector3d toStar = new Vector3d(star.getPosition(gameTime, 0f, oc.registry()))
                .sub(celestial.getPosition(gameTime, 0f, oc.registry()))
                .normalize();

        Quaterniondc rot = new Quaterniond(oc.getRotation()).conjugate();
        toStar.rotate(rot);

        return GenesisMod.UP.dot(toStar);
    }
}
