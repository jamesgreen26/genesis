package shipwrights.genesis.space.type;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.renderer.CelestialRenderer;

public class BuiltinCelestialTypes {

    public static CelestialType STAR = new CelestialType() {
        public boolean castsLight() { return true; }
        public boolean castsShadow() { return false; }
        public boolean isVisitable() { return false; }

        public @NotNull CelestialRenderer getRenderer() {
            return null; //fixme
        }

        public @NotNull ResourceLocation getID() {
            return ResourceLocation.parse("genesis:star");
        }
    };

    public static CelestialType BODY = new CelestialType() {
        public boolean castsLight() { return false; }
        public boolean castsShadow() { return true; }
        public boolean isVisitable() { return true; }

        public @NotNull CelestialRenderer getRenderer() {
            return null; //fixme
        }

        public @NotNull ResourceLocation getID() {
            return ResourceLocation.parse("genesis:body");
        }
    };

    public void register() {
        CelestialType.register(STAR);
        CelestialType.register(BODY);
    }
}
