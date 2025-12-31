package shipwrights.genesis.block.datagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlockDataGenerator {

    public static String FOLDER = "src/main/resources/";


    private static final Logger log = LoggerFactory.getLogger(BlockDataGenerator.class);

    public static void main(String[] args) {
        String name = "voidstone";
        List<BlockType> types = List.of(BlockType.slab, BlockType.stair);

        generate(name, types);
    }

    private static void generate(String name, List<BlockType> types) {
        try {
            for (var type : types) {
                switch (type) {
                    case slab -> SlabGenerator.generateSlab(name);
                    case stair -> StairGenerator.generateStair(name);
                    default -> throw new IllegalArgumentException("unimplemented block type" + type);
                }
            }
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }
}


enum BlockType {
    simple, slab, stair, pillar, fence, wall
}
