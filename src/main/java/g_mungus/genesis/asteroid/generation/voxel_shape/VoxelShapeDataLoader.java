package g_mungus.genesis.asteroid.generation.voxel_shape;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import g_mungus.genesis.asteroid.AsteroidBlock;
import g_mungus.genesis.asteroid.generation.voxel_shape.bitset.BitSetDeserializer;
import g_mungus.genesis.asteroid.generation.voxel_shape.bitset.BitSetSerializer;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.io.InputStream;
import java.util.BitSet;

import static g_mungus.genesis.asteroid.generation.AsteroidGenerator.ASTEROID_COUNT;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class VoxelShapeDataLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SubscribeEvent
    public static void loadAllFromDisk(FMLCommonSetupEvent event) {
        for (int i = 0; i < ASTEROID_COUNT; i++) {
            try {
                ArrayVoxelShape shape = load("asteroid/voxel_shape/asteroid_" + i + ".json").get();
                AsteroidBlock.asteroidShapes.add(shape);
            } catch (Exception e) {
                System.out.println("failed to create voxel shape for asteroid: " + i);
            }
        }
    }

     static ArrayVoxelShapeWrapper load(String filename) {

        SimpleModule module = new SimpleModule();

        module.addSerializer(BitSet.class, new BitSetSerializer());
        module.addDeserializer(BitSet.class, new BitSetDeserializer());
        MAPPER.registerModule(module);


        try (InputStream stream = VoxelShapeDataLoader.class.getResourceAsStream("/" + filename)) {
            if (stream == null) {
                throw new IllegalStateException("Couldn't find resource file!");
            }

            return MAPPER.readValue(stream, ArrayVoxelShapeWrapper.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static JSON data", e);
        }
    }
}
