package shipwrights.dataplanets.systemCreation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record PlanetData(
        String name,
        double size,
        double distanceFromStar,
        double atmosphericDensity,
        double orbitalPeriod,
        double weirdness,
        double seaLevel,
        double temperature,
        double terrainRoughness,
        double flavour,
        ResourceLocation primaryBlock,
        ResourceLocation primaryFluid,
        double gravity
) {

    public static final Codec<PlanetData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("name").forGetter(PlanetData::name),
            Codec.DOUBLE.fieldOf("size").forGetter(PlanetData::size),
            Codec.DOUBLE.fieldOf("distanceFromStar").forGetter(PlanetData::distanceFromStar),
            Codec.DOUBLE.fieldOf("atmosphericDensity").forGetter(PlanetData::atmosphericDensity),
            Codec.DOUBLE.fieldOf("orbitalPeriod").forGetter(PlanetData::orbitalPeriod),
            Codec.DOUBLE.fieldOf("weirdness").forGetter(PlanetData::weirdness),
            Codec.DOUBLE.fieldOf("seaLevel").forGetter(PlanetData::seaLevel),
            Codec.DOUBLE.fieldOf("temperature").forGetter(PlanetData::temperature),
            Codec.DOUBLE.fieldOf("terrainRoughness").forGetter(PlanetData::terrainRoughness),
            Codec.DOUBLE.fieldOf("flavour").forGetter(PlanetData::flavour),
            ResourceLocation.CODEC.fieldOf("primaryBlock").forGetter(PlanetData::primaryBlock),
            ResourceLocation.CODEC.fieldOf("primaryFluid").forGetter(PlanetData::primaryFluid),
            Codec.DOUBLE.fieldOf("gravity").forGetter(PlanetData::gravity)
        ).apply(instance, PlanetData::new)
    );

    public static PlanetData fromPlanetSource(PlanetSource source, String name) {
        ResourceLocation primaryBlock = derivePrimaryBlock(source.temperature(), source.weirdness());
        ResourceLocation primaryFluid = derivePrimaryFluid(source.temperature(), source.seaLevel(), source.atmosphericDensity());
        double gravity = deriveGravity(source.size());

        return new PlanetData(
            name,
            source.size(),
            source.distanceFromStar(),
            source.atmosphericDensity(),
            source.orbitalPeriod(),
            source.weirdness(),
            source.seaLevel(),
            source.temperature(),
            source.terrainRoughness(),
            source.flavour(),
            primaryBlock,
            primaryFluid,
            gravity
        );
    }

    private static ResourceLocation derivePrimaryBlock(double temperature, double weirdness) {
        // Derive primary block based on temperature and weirdness
        if (temperature > 2.0) {
            return ResourceLocation.parse("minecraft:netherrack");
        } else if (temperature > 1.5) {
            return ResourceLocation.parse("minecraft:red_sandstone");
        } else if (temperature < 0.3) {
            return ResourceLocation.parse("minecraft:packed_ice");
        } else if (weirdness > 1.5) {
            return ResourceLocation.parse("minecraft:end_stone");
        } else {
            return ResourceLocation.parse("minecraft:stone");
        }
    }

    private static ResourceLocation derivePrimaryFluid(double temperature, double seaLevel, double atmosphericDensity) {

        if (seaLevel < 0.3 || atmosphericDensity < 0.3) {
            return ResourceLocation.parse("minecraft:air");
        }

        if (temperature < 0.4) {
            return ResourceLocation.parse("minecraft:ice");
        }

        if (temperature > 1.8) {
            return ResourceLocation.parse("minecraft:lava");
        }

        return ResourceLocation.parse("minecraft:water");
    }

    private static double deriveGravity(double size) {
        // Gravity is primarily based on planet size (mass)
        return Math.sqrt(size);
    }
}
