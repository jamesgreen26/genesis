package shipwrights.genesis.mixin_extension;

import org.joml.Vector3d;

public interface FallingBlockEntityExtension {
    void genesis$setRotation(Vector3d rotation);
    Vector3d genesis$getRotation();
}
