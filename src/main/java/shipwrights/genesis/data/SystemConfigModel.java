package shipwrights.genesis.data;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

public record SystemConfigModel(List<PlanetJsonModel> planets, List<MoonJsonModel> moons) {

    public static final Codec<SystemConfigModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlanetJsonModel.CODEC.listOf().fieldOf("planets").forGetter(SystemConfigModel::planets),
            MoonJsonModel.CODEC.listOf().fieldOf("moons").forGetter(SystemConfigModel::moons)
    ).apply(instance, SystemConfigModel::new));

    public SystemConfigModel {
        planets = planets != null ? planets : Collections.emptyList();
        moons = moons != null ? moons : Collections.emptyList();
    }

    public record PlanetJsonModel(String dimensionID, double size, double orbitRadius, double yearLength, double gravity, float r, float g, float b) {

        public static final Codec<PlanetJsonModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimensionID").forGetter(PlanetJsonModel::dimensionID),
                Codec.DOUBLE.fieldOf("size").forGetter(PlanetJsonModel::size),
                Codec.DOUBLE.fieldOf("orbitRadius").forGetter(PlanetJsonModel::orbitRadius),
                Codec.DOUBLE.fieldOf("yearLength").forGetter(PlanetJsonModel::yearLength),
                Codec.DOUBLE.fieldOf("gravity").forGetter(PlanetJsonModel::gravity),
                Codec.FLOAT.fieldOf("r").forGetter(PlanetJsonModel::r),
                Codec.FLOAT.fieldOf("g").forGetter(PlanetJsonModel::g),
                Codec.FLOAT.fieldOf("b").forGetter(PlanetJsonModel::b)
        ).apply(instance, PlanetJsonModel::new));

        public ResourceLocation getDimensionID() {
            return ResourceLocation.parse(dimensionID);
        }
    }

    public record MoonJsonModel(String dimensionID, String parentDimensionID, double size, double orbitRadius, double yearLength, double gravity, float r, float g, float b) {

        public static final Codec<MoonJsonModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimensionID").forGetter(MoonJsonModel::dimensionID),
                Codec.STRING.fieldOf("parentDimensionID").forGetter(MoonJsonModel::parentDimensionID),
                Codec.DOUBLE.fieldOf("size").forGetter(MoonJsonModel::size),
                Codec.DOUBLE.fieldOf("orbitRadius").forGetter(MoonJsonModel::orbitRadius),
                Codec.DOUBLE.fieldOf("yearLength").forGetter(MoonJsonModel::yearLength),
                Codec.DOUBLE.fieldOf("gravity").forGetter(MoonJsonModel::gravity),
                Codec.FLOAT.fieldOf("r").forGetter(MoonJsonModel::r),
                Codec.FLOAT.fieldOf("g").forGetter(MoonJsonModel::g),
                Codec.FLOAT.fieldOf("b").forGetter(MoonJsonModel::b)
        ).apply(instance, MoonJsonModel::new));

        public ResourceLocation getDimensionID() {
            return ResourceLocation.parse(dimensionID);
        }

        public ResourceLocation getParentDimensionID() {
            return ResourceLocation.parse(parentDimensionID);
        }
    }
}


