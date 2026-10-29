package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.MoreCodecs;

public record StaticTransformProvider(
        Vector3dc position,
        Vector3dc rotation
) implements Celestial.TransformProvider {

    static ResourceLocation TYPE = GenesisMod.id("static");

    @Override
    public Vector3dc getPosition(Registry<Celestial> celestials, Time time) {
        return position;
    }

    @Override
    public Quaterniondc getRotation(Registry<Celestial> celestials, Time time) {
        return new Quaterniond().rotateXYZ(rotation.x(), rotation.y(), rotation.z());
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    public static final Codec<StaticTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MoreCodecs.VECTOR3DC.fieldOf("position").forGetter(StaticTransformProvider::position),
                    MoreCodecs.VECTOR3DC.fieldOf("rotation").forGetter(StaticTransformProvider::rotation)
            ).apply(instance, StaticTransformProvider::new)
    );
}
