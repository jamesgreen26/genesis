package shipwrights.genesis.content.block.datagen;

public class BlockItemGenerator {
    public static void generate(String name) {
        String json = """
            {
              "parent": "genesis:block/%1$s"
            }
            """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "assets/genesis/models/item/" + name + ".json";

        FileWriter.writeFile(path, json);
    }
}
