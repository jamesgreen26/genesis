package shipwrights.genesis.util;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class MoreCodecs {
    public static final Codec<Vector3dc> VECTOR3DC
            = Codec.DOUBLE.listOf().comapFlatMap(p_253502_ -> Util.fixedSize(p_253502_, 3).map(p_253489_ -> new Vector3d(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2))), p_269787_ -> List.of(p_269787_.x(), p_269787_.y(), p_269787_.z()));

}
