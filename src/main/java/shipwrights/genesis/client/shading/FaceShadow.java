package shipwrights.genesis.client.shading;

import org.joml.Vector2dc;
import shipwrights.genesis.math.AAPlane;

import java.util.List;

public record FaceShadow(AAPlane plane, List<Vector2dc> polygon) {}

