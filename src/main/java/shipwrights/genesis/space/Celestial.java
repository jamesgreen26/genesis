package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kotlin.Pair;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.space.type.CelestialType;

import java.util.function.Predicate;

public class Celestial {
    public static double BASE_SIZE = 96;
    public static double BASE_ORBIT_DISTANCE = 15_000;
    public static double BASE_ORBIT_TIME = 4_608_000;
    public static double BASE_DAY_LENGTH = 24_000;

    private static final Vector3dc EAST = VectorConversionsMCKt.toJOMLD(Direction.EAST.getNormal());
    private static final Vector3dc UP = VectorConversionsMCKt.toJOMLD(Direction.UP.getNormal());

    private final CelestialTransformProvider transformProvider;
    private final ResourceLocation ID;
    private final CelestialType type;
    private final double size;
    private final double gravity;
    private final float r;
    private final float g;
    private final float b;

    public Celestial(CelestialTransformProvider transformProvider, ResourceLocation id, CelestialType type, double size, double gravity, float r, float g, float b) {
        this.transformProvider = transformProvider;
        this.ID = id;
        this.type = type;
        this.size = size;
        this.gravity = gravity;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public ResourceLocation getID() {
        return ID;
    }

    public CelestialType getType() {
        return type;
    }

    public Vector3dc getPosition(long ticks, float partialTick) {
        return transformProvider.getPosition(ticks, partialTick);
    }

    public Vector3dc getPosition(long ticks) {
        return getPosition(ticks, 0f);
    }

    public double size() {
        return size;
    }

    public double getActualSize() {
        return this.size();
    }

    public OBB getOBB(long ticks) {
        return getOBB(ticks, 0);
    }

    public OBB getOBB(long ticks, float subticks) {
        return OBB.createCube(getActualSize(), getRotation(ticks, subticks), getPosition(ticks, subticks));
    }

    public Quaterniondc getRotation(long ticks, float partialTick) {
        return transformProvider.getRotation(ticks, partialTick);
    }

    public Quaterniondc getRotation(long ticks) { return getRotation(ticks, 0f); }

    public CelestialTransformProvider getTransformProvider() {
        return transformProvider;
    }

    public double gravity() {
        return gravity;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }


    public Celestial getNearestStar(long gameTime, float partialTick) {
        if (BuiltinCelestialTypes.STAR.equals(getType())) {
            return this;
        } else {
            Pair<Celestial, Double> result = SpaceLevel.nearestCelestialWhere(getPosition(gameTime, partialTick), gameTime, partialTick, Predicate.isEqual(BuiltinCelestialTypes.STAR));
            if (result != null) {
                return result.getFirst();
            } else {
                throw new IllegalStateException("Why are there no stars??");
            }
        }
    }

    public long getDayTime(long gameTime) {
        return getDayTime(gameTime, 0);
    }

    public long getDayTime(long gameTime, float partialTick) {
        Celestial star = getNearestStar(gameTime, partialTick);

        Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick))
                .sub(getPosition(gameTime, partialTick))
                .normalize();

        Quaterniondc rot = new Quaterniond().rotateX(- Math.PI/2).premul(getRotation(gameTime, partialTick));

        Vector3d up = UP.rotate(rot, new Vector3d());
        Vector3d east = EAST.rotate(rot, new Vector3d());

        double angle = Math.atan2(-east.dot(toStar), up.dot(toStar));

        double d = (angle / (2.0 * Math.PI) + 1.0) % 1.0;

        d = (d + 0.25) % 1.0;

        return Math.round(d * 24000.0);
    }

    public double getSunDot(long gameTime, float partialTick) {
        Celestial star = getNearestStar(gameTime, partialTick);

        Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick))
                .sub(getPosition(gameTime, partialTick))
                .normalize();

        Quaterniondc rot = new Quaterniond().rotateX(- Math.PI/2).premul(getRotation(gameTime, partialTick));

        Vector3d up = UP.rotate(rot, new Vector3d());
        return up.dot(toStar);
    }

    public static final Codec<Celestial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ID").forGetter(it -> it.ID.toString()),
            Codec.STRING.fieldOf("type").forGetter(it -> it.type.toString()),
            Codec.DOUBLE.fieldOf("size").forGetter(Celestial::size),
            Codec.DOUBLE.fieldOf("gravity").forGetter(Celestial::gravity),
            Codec.FLOAT.optionalFieldOf("r", 0.5f).forGetter(Celestial::r),
            Codec.FLOAT.optionalFieldOf("g", 0.5f).forGetter(Celestial::g),
            Codec.FLOAT.optionalFieldOf("b", 0.5f).forGetter(Celestial::b),
            CelestialTransformProvider.DISPATCH_CODEC.fieldOf("transformProvider").forGetter(
                    Celestial::getTransformProvider
            )
    ).apply(instance, (id, type, size, gravity, r, g, b, transformProvider) ->
            new Celestial(transformProvider, ResourceLocation.parse(id), CelestialType.get(ResourceLocation.parse(type)), size, gravity, r, g, b)
    ));

}
