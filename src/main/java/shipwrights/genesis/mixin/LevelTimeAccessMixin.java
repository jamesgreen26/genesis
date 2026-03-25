package shipwrights.genesis.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelTimeAccess;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

@Mixin(LevelTimeAccess.class)
public interface LevelTimeAccessMixin extends LevelReader {

    /**
     * @author Genesis
     * @reason Replace with apparent sun angle based on celestial position
     */
    @Overwrite
    default float getTimeOfDay(float partialTick) {
        if (this instanceof Level level) {
            long gameTime = GenesisMod.getTicks(level);
            VantagePoint vp = VantagePoint.get(level, new Vector3d(), gameTime, partialTick);

            if (vp instanceof VantagePoint.OnCelestial oc) {
                Celestial celestial = oc.celestial();
                Celestial star = celestial.getNearestStar(gameTime, partialTick, oc.registry());

                Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick, oc.registry()))
                        .sub(celestial.getPosition(gameTime, partialTick, oc.registry()))
                        .normalize();

                Quaterniondc rot = new Quaterniond(oc.getRotation()).conjugate();
                toStar.rotate(rot);

                double apparentAngle = GenesisMod.getApparentSunAngle(
                        GenesisMod.UP.dot(toStar),
                        GenesisMod.EAST.dot(toStar));
                return level.dimensionType().timeOfDay((long)(apparentAngle * 24000));
            }
        }
        return this.dimensionType().timeOfDay(this.dayTime());
    }

    @Shadow
    long dayTime();
}
