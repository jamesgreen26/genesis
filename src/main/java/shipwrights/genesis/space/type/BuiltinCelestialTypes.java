package shipwrights.genesis.space.type;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.renderer.CelestialRenderer;
import shipwrights.genesis.space.renderer.PlanetRenderer;
import shipwrights.genesis.space.renderer.StarRenderer;

public class BuiltinCelestialTypes {

    public static CelestialType STAR = new CelestialType() {
        public boolean castsLight() { return true; }
        public boolean castsShadow() { return false; }
        public boolean isVisitable() { return false; }

        private static CelestialRenderer renderer = null;
        public @NotNull CelestialRenderer getRenderer() {
            CelestialRenderer result = renderer;
            if (result == null) {
                renderer = new StarRenderer();
            }
            return renderer;
        }

        public @NotNull ResourceLocation getID() {
            return ResourceLocation.parse("genesis:star");
        }
    };

    public static CelestialType BODY = new CelestialType() {
        public boolean castsLight() { return false; }
        public boolean castsShadow() { return true; }
        public boolean isVisitable() { return true; }

        private static CelestialRenderer renderer = null;
        public @NotNull CelestialRenderer getRenderer() {
            CelestialRenderer result = renderer;
            if (result == null) {
                renderer = new PlanetRenderer();
            }
            return renderer;
        }

        public @NotNull ResourceLocation getID() {
            return ResourceLocation.parse("genesis:body");
        }
    };

    public static void register() {
        CelestialType.register(STAR);
        CelestialType.register(BODY);
    }
}
