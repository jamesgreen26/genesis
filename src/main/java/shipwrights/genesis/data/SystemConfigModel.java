package shipwrights.genesis.data;


import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SystemConfigModel(List<PlanetJsonModel> planets) {

    public record PlanetJsonModel(String dimensionID, double size, double sunDist, double yearLength, float r, float g, float b) {

        public ResourceLocation getDimensionID() {
            return ResourceLocation.parse(dimensionID);
        }
    }
}


