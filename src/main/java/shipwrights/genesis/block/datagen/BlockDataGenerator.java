package shipwrights.genesis.block.datagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class BlockDataGenerator {

    public static String FOLDER = "src/main/resources/";
    private static final Logger log = LoggerFactory.getLogger(BlockDataGenerator.class);

    public static final Map<String, List<BlockType>> blocksToDatagen = Map.of(
            "voidstone", List.of(BlockType.simple, BlockType.slab, BlockType.stair),
            "nullstone", List.of(BlockType.simple, BlockType.slab, BlockType.stair),
            "riftrock", List.of(BlockType.simple, BlockType.slab, BlockType.stair)
    );

    public static void main(String[] args) {
        blocksToDatagen.forEach(BlockDataGenerator::generate);
    }

    private static void generate(String name, List<BlockType> types) {
        try {
            for (var type : types) {
                switch (type) {
                    case slab -> SlabGenerator.generateSlab(name);
                    case stair -> StairGenerator.generateStair(name);
                    default -> {}
                }
            }
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }
}
