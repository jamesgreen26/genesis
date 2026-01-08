package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.TestOnly;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

/**
 * If you use this, the planet transform will not be synchronized to the client.
 * <br> <br>
 * This is currently intended for test purposes only. It may be fully implemented as a feature later.
 **/

@TestOnly
public interface CustomTransformProvider {
    Quaterniondc getRotation();
    Vector3d getCurrentPos(long ticks, float subticks);
    <T extends CustomTransformProvider> Codec<T> getCodec();
}
