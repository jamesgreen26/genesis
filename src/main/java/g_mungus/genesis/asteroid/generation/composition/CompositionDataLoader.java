package g_mungus.genesis.asteroid.generation.composition;

import com.fasterxml.jackson.databind.*;
import g_mungus.genesis.asteroid.AsteroidBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.io.InputStream;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CompositionDataLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SubscribeEvent
    public static void loadAllFromDisk(FMLCommonSetupEvent event) {
        for (int i = 0; i < ASTEROID_COUNT; i++) {
            try {
                CompositionData compositionData = load("asteroid/composition/asteroid_" + i + ".json");
                AsteroidBlock.compositions.put(i, compositionData);
            } catch (Exception e) {
                System.out.println("failed to load composition for asteroid: " + i);
            }
        }
    }

    static CompositionData load(String filename) {

        try (InputStream stream = CompositionDataLoader.class.getResourceAsStream("/" + filename)) {
            if (stream == null) {
                throw new IllegalStateException("Couldn't find resource file!");
            }

            return MAPPER.readValue(stream, CompositionData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static JSON data", e);
        }
    }
}