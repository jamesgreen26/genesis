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
import shipwrights.genesis.space.planet_properties.PlanetProperties;

import java.lang.Math;

@Mixin(value = FogRenderer.class, priority = 1100)
public class FogRendererMixin {
    
    @WrapOperation(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 genesis$getSkyColor(ClientLevel instance, Vec3 pos, float partialTick, Operation<Vec3> original,
                                            @Share("apparentAngle") LocalDoubleRef apparentAngle) {
        Celestial vantagePoint = GenesisMod.getCelestialForLevel(instance);
        if (vantagePoint != null) {
            long gameTime = GenesisMod.getTicks(instance);
            
            Celestial star = vantagePoint.getNearestStar(gameTime, partialTick);
            Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick))
                    .sub(vantagePoint.getPosition(gameTime, partialTick))
                    .normalize();
            
            Quaterniondc rot = new Quaterniond(vantagePoint.getRotation(gameTime, partialTick)).rotateX(-Math.PI/2).conjugate();
            toStar.rotate(rot);
            
            double starUpDot = PlanetDimensionEffects.UP.dot(toStar);
            double starEastDot = PlanetDimensionEffects.EAST.dot(toStar);
            
            double _apparentAngle = PlanetDimensionEffects.getApparentSunAngle(starUpDot, starEastDot);
            apparentAngle.set(_apparentAngle);
            long fakeTime = (long) (_apparentAngle * 24000);
            
            return PlanetDimensionEffects.getSkyColor(pos, partialTick, fakeTime, instance, PlanetProperties.get(vantagePoint.getID()).color());
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
        Celestial vantagePoint = GenesisMod.getCelestialForLevel(level);
        if (vantagePoint != null) {
            long gameTime = GenesisMod.getTicks(level);
            
            Celestial star = vantagePoint.getNearestStar(gameTime, partialTick);
            Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick))
                    .sub(vantagePoint.getPosition(gameTime, partialTick))
                    .normalize();
            
            Quaterniondc rot = new Quaterniond(vantagePoint.getRotation(gameTime, partialTick)).rotateX(-Math.PI/2).conjugate();
            toStar.rotate(rot);
            
            ((Vector3f) v).set(toStar);
        }
        return original.call(instance, v);
        
    }
    
}
