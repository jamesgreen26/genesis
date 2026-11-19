package shipwrights.genesis.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.util.schema.SchemaProbe;

import java.util.HashMap;

/// The codecs a dispatch codec picks between, by type id, as used with
/// {@link com.mojang.serialization.Codec#dispatchStable}. Just a map, except that {@link SchemaProbe} can tell it's a
/// dispatch and find out every type it could be, so editors built from codecs offer them all.
public class CodecRegistry<T> extends HashMap<ResourceLocation, MapCodec<? extends T>> {

    @Override
    public MapCodec<? extends T> get(Object key) {
        if (SchemaProbe.isProbeId(key)) {
            return SchemaProbe.dispatchedTo(this);
        }
        return super.get(key);
    }
}
