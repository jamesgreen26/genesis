package shipwrights.genesis.space.properties;

import com.mojang.serialization.Codec;

/** Properties for celestial types that need no extra data (e.g. black holes). */
public record EmptyProperties() implements CelestialProperties {
    public static final EmptyProperties INSTANCE = new EmptyProperties();
    public static final Codec<EmptyProperties> CODEC = Codec.unit(INSTANCE);
}
