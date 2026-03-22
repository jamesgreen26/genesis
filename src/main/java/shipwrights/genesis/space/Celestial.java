package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kotlin.Pair;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.space.type.CelestialType;

import java.util.function.Predicate;

public record Celestial(
        CelestialTransformProvider transformProvider,
        ResourceLocation ID,
        CelestialType type,
        double size,
        double gravity,
        float r,
        float g,
        float b
) {
    public Vector3dc getPosition(long ticks, float partialTick) {
        return transformProvider.getPosition(ticks, partialTick);
    }

    public Vector3dc getPosition(long ticks) {
        return getPosition(ticks, 0f);
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

    public Quaterniondc getRotation(long ticks) {
        return getRotation(ticks, 0f);
    }

    public Celestial getNearestStar(long gameTime, float partialTick) {
        if (BuiltinCelestialTypes.STAR.equals(type())) {
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

    public static final Codec<Celestial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ID").forGetter(it -> it.ID.toString()),
            Codec.STRING.fieldOf("type").forGetter(it -> it.type.getID().toString()),
            Codec.DOUBLE.fieldOf("size").forGetter(Celestial::size),
            Codec.DOUBLE.fieldOf("gravity").forGetter(Celestial::gravity),
            Codec.FLOAT.optionalFieldOf("r", 0.5f).forGetter(Celestial::r),
            Codec.FLOAT.optionalFieldOf("g", 0.5f).forGetter(Celestial::g),
            Codec.FLOAT.optionalFieldOf("b", 0.5f).forGetter(Celestial::b),
            CelestialTransformProvider.DISPATCH_CODEC.fieldOf("transformProvider").forGetter(
                    Celestial::transformProvider
            )
    ).apply(instance, (id, type, size, gravity, r, g, b, transformProvider) ->
            new Celestial(transformProvider, ResourceLocation.parse(id), CelestialType.get(ResourceLocation.parse(type)), size, gravity, r, g, b)
    ));
}
