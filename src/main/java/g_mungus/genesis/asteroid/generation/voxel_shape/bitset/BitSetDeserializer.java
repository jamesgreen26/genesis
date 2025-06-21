package g_mungus.genesis.asteroid.generation.voxel_shape.bitset;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.BitSet;

public class BitSetDeserializer extends JsonDeserializer<BitSet> {
    @Override
    public BitSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        BitSet bitSet = new BitSet();
        JsonNode node = p.getCodec().readTree(p);
        for (JsonNode indexNode : node) {
            bitSet.set(indexNode.asInt());
        }
        return bitSet;
    }
}
