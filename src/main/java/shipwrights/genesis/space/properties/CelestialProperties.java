package shipwrights.genesis.space.properties;

/**
 * Marker interface for type-safe celestial properties.
 * Each {@link shipwrights.genesis.space.type.CelestialType} provides a codec that decodes
 * the appropriate subtype from the {@code "properties"} block in the celestial JSON.
 */
public interface CelestialProperties {
}
