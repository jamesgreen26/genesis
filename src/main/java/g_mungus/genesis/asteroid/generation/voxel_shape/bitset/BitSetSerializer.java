package g_mungus.genesis.asteroid.generation.voxel_shape.bitset;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.BitSet;

public class BitSetSerializer extends JsonSerializer<BitSet> {
    @Override
    public void serialize(BitSet value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        for (int i = value.nextSetBit(0); i >= 0; i = value.nextSetBit(i + 1)) {
            gen.writeNumber(i);
        }
        gen.writeEndArray();
    }
}

