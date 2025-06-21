package g_mungus.genesis.asteroid.generation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class AsteroidDataLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ArrayVoxelShapeWrapper load(String filename) {
        try (InputStream stream = AsteroidDataLoader.class.getResourceAsStream("/" + filename)) {
            if (stream == null) {
                throw new IllegalStateException("Couldn't find resource file!");
            }

            return MAPPER.readValue(stream, ArrayVoxelShapeWrapper.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static JSON data", e);
        }
    }
}
