package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Optional;
import java.util.Random;

public final class OrbitingBody extends Orbitable.Celestial {

    private static final Vector3dc EAST = VectorConversionsMCKt.toJOMLD(Direction.EAST.getNormal());
    private static final Vector3dc UP = VectorConversionsMCKt.toJOMLD(Direction.UP.getNormal());

    private final String dimensionID;
    private final String parentID;
    private final double size;
    private final double orbitDistance;
    private final double orbitTime;
    private final double orbitalTheta;
    private final double orbitalPhi;
    private final double gravity;
    private final float r;
    private final float g;
    private final float b;

    private @Nullable Orbitable parent = null;
    private final Quaterniondc rotation;

    private final @Nullable CustomTransformProvider customTransformProvider;
    private final double dayLength;

    public OrbitingBody(String ID, String parentID, double size, double orbitDistance, double orbitTime, double gravity, float r, float g, float b) {
        this(ID, parentID, size, orbitDistance, orbitTime, gravity, r, g, b, 1.0, null);
    }

    @TestOnly
    public OrbitingBody(String ID, String parentID, double size, double orbitDistance, double orbitTime, double gravity, float r, float g, float b, double dayLength, @Nullable CustomTransformProvider customTransformProvider) {
        this.dimensionID = ID;
        this.parentID = parentID;
        this.size = size;
        this.orbitDistance = orbitDistance;
        this.orbitTime = orbitTime;
        this.gravity = gravity;
        this.r = r;
        this.g = g;
        this.b = b;
        this.dayLength = Math.max(0.001, dayLength);
        this.customTransformProvider = customTransformProvider;

        Random rand = new Random(ID.hashCode());
        for (int i = 0; i < rand.nextInt(10); i++) {
            rand.nextDouble();
        }
        if (dimensionID.equals("minecraft:overworld")) {
            this.rotation = new Quaterniond();
        } else {
            this.rotation = new Quaterniond().rotationXYZ(rand.nextDouble(Math.PI), rand.nextDouble(Math.PI), rand.nextDouble(Math.PI));
        }
        this.orbitalTheta = rand.nextDouble() * 2 * Math.PI;   // longitude
        this.orbitalPhi   = (Math.acos(2 * rand.nextDouble() - 1) + Math.PI) / 3; // latitude
    }

    @Override
    public boolean exists() {
        return super.exists() && this.parent != null && this.parent.exists();
    }

    public Orbitable getParent() {
        assert parent != null;
        return parent;
    }

    public void defineParent(Orbitable parent) {
        if (this.parent == null) {
            this.parent = parent;
        }
    }

    @Override
    public Vector3d getCurrentPos(long ticks, float subticks) {
        if (this.customTransformProvider != null) {
            return customTransformProvider.getCurrentPos(ticks, subticks, getParent());
        }
        Vector3d out = new Vector3d(1, 0, 0);
        out = out.rotateY(Math.PI * 2 * (ticks + subticks) / getYearLengthTicks());
        // out = out.rotateY(orbitalTheta);
        // out = out.rotateX(orbitalPhi + Math.PI / 2);
        out.normalize(orbitDistance * BASE_ORBIT_DISTANCE);
        return out.add(getParent().getCurrentPos(ticks, subticks), new Vector3d());
    }

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.parse(dimensionID);
    }

    public ResourceLocation getParentID() {
        return ResourceLocation.parse(parentID);
    }

    public int getYearLengthTicks() {
        return (int)(this.orbitTime * BASE_ORBIT_TIME);
    }

    private String dimensionID() { return dimensionID; }
    private String parentID() { return parentID; }
    public double size() { return size; }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        if (this.customTransformProvider != null) {
            return customTransformProvider.getRotation(ticks, subticks, getParent());
        }

        return new Quaterniond(rotation).rotateZ(-Math.PI * 2 * (ticks + subticks) / (this.dayLength * BASE_DAY_LENGTH));
    }

    public double orbitRadius() { return orbitDistance; }
    public double yearLength() { return orbitTime; }
    public double dayLength() { return dayLength; }
    public double gravity() { return gravity; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }

    @Nullable
    public CustomTransformProvider customTransformProvider() {
        return customTransformProvider;
    }

    public static final Codec<OrbitingBody> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ID").forGetter(OrbitingBody::dimensionID),
            Codec.STRING.fieldOf("parentID").forGetter(OrbitingBody::parentID),
            Codec.DOUBLE.fieldOf("size").forGetter(OrbitingBody::size),
            Codec.DOUBLE.fieldOf("orbitDistance").forGetter(OrbitingBody::orbitRadius),
            Codec.DOUBLE.fieldOf("orbitTime").forGetter(OrbitingBody::yearLength),
            Codec.DOUBLE.fieldOf("gravity").forGetter(OrbitingBody::gravity),
            Codec.FLOAT.optionalFieldOf("r", 0.5f).forGetter(OrbitingBody::r),
            Codec.FLOAT.optionalFieldOf("g", 0.5f).forGetter(OrbitingBody::g),
            Codec.FLOAT.optionalFieldOf("b", 0.5f).forGetter(OrbitingBody::b),
            Codec.DOUBLE.optionalFieldOf("dayLength", 1.0).forGetter(OrbitingBody::dayLength),
            CustomTransformProvider.DISPATCH_CODEC.optionalFieldOf("customTransform").forGetter(
                body -> Optional.ofNullable(body.customTransformProvider())
            )
    ).apply(instance, (id, parentId, size, orbitDist, orbitTime, grav, r, g, b, dayLength, customTransform) ->
        new OrbitingBody(id, parentId, size, orbitDist, orbitTime, grav, r, g, b, dayLength, customTransform.orElse(null))
    ));

    public WithDistanceSq withDistanceSq(double distanceSquared) {
        return new WithDistanceSq(this, distanceSquared);
    }

    public Star getStar() {
        Orbitable starCandidate = this;
        while (!(starCandidate instanceof Star)) {
            starCandidate = ((OrbitingBody) starCandidate).getParent();
        }
        return (Star) starCandidate;
    }

    public long getDayTime(long gameTime) {
        return getDayTime(gameTime, 0);
    }

    public long getDayTime(long gameTime, float subtick) {
        Star star = getStar();

        Vector3d toStar = new Vector3d(star.getCurrentPos(gameTime, subtick))
                .sub(getCurrentPos(gameTime, subtick))
                .normalize();

        Quaterniondc rot = getRotation(gameTime, subtick);

        Vector3d up = UP.rotate(rot, new Vector3d());
        Vector3d east = EAST.rotate(rot, new Vector3d());

        double angle = Math.atan2(-east.dot(toStar), up.dot(toStar));

        double d = (angle / (2.0 * Math.PI) + 1.0) % 1.0;

        d = (d + 0.25) % 1.0;

        return Math.round(d * 24000.0);
    }

    public static class WithDistanceSq extends Celestial.WithDistanceSq<OrbitingBody> {
        public WithDistanceSq(OrbitingBody celestial, double distanceSquared) {
            super(celestial, distanceSquared);
        }
    }
}
