package shipwrights.genesis.data;


import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SystemConfigModel(List<PlanetJsonModel> planets, List<MoonJsonModel> moons) {

    public record PlanetJsonModel(String dimensionID, double size, double orbitRadius, double yearLength, double gravity, float r, float g, float b) {
        public ResourceLocation getDimensionID() {
            return ResourceLocation.parse(dimensionID);
        }
    }

    public record MoonJsonModel(String dimensionID, String parentDimensionID, double size, double orbitRadius, double yearLength, double gravity, float r, float g, float b) {
        public ResourceLocation getDimensionID() {
            return ResourceLocation.parse(dimensionID);
        }

        public ResourceLocation getParentDimensionID() {
            return ResourceLocation.parse(parentDimensionID);
        }
    }
}


