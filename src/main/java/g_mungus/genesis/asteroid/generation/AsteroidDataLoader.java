package g_mungus.genesis.asteroid.generation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

public class AsteroidDataLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ArrayVoxelShapeWrapper load(String filename) {

        SimpleModule module = new SimpleModule();

        module.addSerializer(BitSet.class, new BitSetSerializer());
        module.addDeserializer(BitSet.class, new BitSetDeserializer());
        MAPPER.registerModule(module);


        try (InputStream stream = AsteroidDataLoader.class.getResourceAsStream("/" + filename)) {
            if (stream == null) {
                throw new IllegalStateException("Couldn't find resource file!");
            }

            return MAPPER.readValue(stream, ArrayVoxelShapeWrapper.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static JSON data", e);
        }
    }

    public static class BitSetSerializer extends JsonSerializer<BitSet> {
        @Override
        public void serialize(BitSet value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (int i = value.nextSetBit(0); i >= 0; i = value.nextSetBit(i + 1)) {
                gen.writeNumber(i);
            }
            gen.writeEndArray();
        }
    }

    public static class BitSetDeserializer extends JsonDeserializer<BitSet> {
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
}
