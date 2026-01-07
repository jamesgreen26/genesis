package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Random;

public final class Star extends Orbitable.Celestial {

    private final String ID;
    private final double temperature;
    private final double size;
    private final double x;
    private final double y;
    private final double z;
    private final Quaterniondc rotation;

    public Star(String ID, double temperature, double size, double x, double y, double z) {
        this.ID = ID;
        this.temperature = temperature;
        this.size = size;
        this.x = x;
        this.y = y;
        this.z = z;

        Random rand = new Random(ID.hashCode());
        this.rotation = new Quaterniond().rotationXYZ(rand.nextDouble(Math.PI), rand.nextDouble(Math.PI), rand.nextDouble(Math.PI));
    }

    @Override
    public Vector3dc getCurrentPos(long ticks, float subticks) {
        return new Vector3d(x, y, z);
    }

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.parse(ID);
    }

    private String ID() { return ID; }
    public double temperature() { return temperature; }
    public double size() { return size; }

    @Override
    public Quaterniondc getRotation() {
        return rotation;
    }

    private double x() { return x; }
    private double y() { return y; }
    private double z() { return z; }

    public static final Codec<Star> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ID").forGetter(Star::ID),
            Codec.DOUBLE.fieldOf("temperature").forGetter(Star::temperature),
            Codec.DOUBLE.fieldOf("size").forGetter(Star::size),
            Codec.DOUBLE.fieldOf("x").forGetter(Star::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Star::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Star::z)
    ).apply(instance, Star::new));

    public WithDistanceSq withDistanceSq(double distanceSquared) {
        return new WithDistanceSq(this, distanceSquared);
    }

    public static class WithDistanceSq extends Celestial.WithDistanceSq<Star> {
        public WithDistanceSq(Star celestial, double distanceSquared) {
            super(celestial, distanceSquared);
        }
    }
}
