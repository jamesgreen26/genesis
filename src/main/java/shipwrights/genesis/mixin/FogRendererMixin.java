package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetColorPalette;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.lang.Math;

@Mixin(value = FogRenderer.class, priority = 1100)
public class FogRendererMixin {
    
    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 genesis$getSkyColor(ClientLevel instance, Vec3 pos, float partialTick, Operation<Vec3> original,
                                            @Share("apparentAngle") LocalDoubleRef apparentAngle) {
        long gameTime = GenesisMod.getTicks(instance);
        VantagePoint vp = VantagePoint.get(instance, new Vector3d(pos.x, pos.y, pos.z), gameTime, partialTick);
        if (vp instanceof VantagePoint.OnCelestial oc) {
            Celestial star = oc.celestial().getNearestStar(gameTime, partialTick, oc.registry());
            Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick, oc.registry()))
                    .sub(oc.celestial().getPosition(gameTime, partialTick, oc.registry()))
                    .normalize();

            Quaterniondc rot = new Quaterniond(oc.getRotation()).conjugate();
            toStar.rotate(rot);

            double starUpDot = GenesisMod.UP.dot(toStar);
            double starEastDot = GenesisMod.EAST.dot(toStar);

            double _apparentAngle = GenesisMod.getApparentSunAngle(starUpDot, starEastDot);
            apparentAngle.set(_apparentAngle);
            long fakeTime = (long) (_apparentAngle * 24000);

            PlanetColorPalette palette = oc.celestial().properties() instanceof PlanetProperties pp
                    ? pp.atmosphere().color() : new PlanetColorPalette.Overworld();
            return PlanetDimensionEffects.getSkyColor(pos, partialTick, fakeTime, instance, palette);
        }
        
        apparentAngle.set(instance.getSunAngle(partialTick) / Mth.TWO_PI);
        return original.call(instance, pos, partialTick);
    }
    
    @Redirect(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSunAngle(F)F"))
    private static float genesis$getSunAngle(ClientLevel instance, float v, @Share("apparentAngle") LocalDoubleRef apparentAngle) {
        return (float) (apparentAngle.get() * Math.PI * 2);
    }
    
    @Redirect(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private static float genesis$getTimeOfDay(ClientLevel instance, float v, @Share("apparentAngle") LocalDoubleRef apparentAngle) {
        return instance.dimensionType().timeOfDay((long) (apparentAngle.get() * 24000));
    }
    
    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;dot(Lorg/joml/Vector3fc;)F", remap = false))
    private static float wrapViewDirectionDot(Vector3f instance, Vector3fc v, Operation<Float> original, @Local(argsOnly = true, ordinal = 0) float partialTick,
                                              @Local(argsOnly = true) ClientLevel level) {
        long gameTime = GenesisMod.getTicks(level);
        VantagePoint vp2 = VantagePoint.get(level, new Vector3d(), gameTime, partialTick);
        if (vp2 instanceof VantagePoint.OnCelestial oc) {
            Celestial star = oc.celestial().getNearestStar(gameTime, partialTick, oc.registry());
            Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick, oc.registry()))
                    .sub(oc.celestial().getPosition(gameTime, partialTick, oc.registry()))
                    .normalize();

            Quaterniondc rot = new Quaterniond(oc.getRotation()).conjugate();
            toStar.rotate(rot);

            ((Vector3f) v).set(toStar);
        }
        return original.call(instance, v);
        
    }
    
}
