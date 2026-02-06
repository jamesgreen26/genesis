package shipwrights.genesis.space.type;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.space.renderer.*;
import shipwrights.genesis.space.renderer.star.StarEffect;

public class BuiltinCelestialTypes {

    public static CelestialType STAR = new CelestialType() {
        public boolean castsLight() { return true; }
        public boolean castsShadow() { return false; }
        public boolean isVisitable() { return false; }

        @Override
        public @NotNull EffectFactory getEffectFactory() {
            return StarEffect::new;
        }

        public @NotNull ResourceLocation getID() {
            return ResourceLocation.parse("genesis:star");
        }
    };

    public static CelestialType BODY = new CelestialType() {
        public boolean castsLight() { return false; }
        public boolean castsShadow() { return true; }
        public boolean isVisitable() { return true; }

        @Override
        public EffectFactory getEffectFactory() {
            // TODO: Implement planet effect with Flywheel
            return null;
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
