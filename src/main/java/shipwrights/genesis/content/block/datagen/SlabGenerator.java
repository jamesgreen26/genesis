package shipwrights.genesis.content.block.datagen;

public class SlabGenerator {
    public static void generateSlab(String name) {
        generateBlockState(name);
        generateBlockModel(name);
        generateLootTable(name);
        BlockItemGenerator.generate(name + "_slab");
    }

    private static void generateBlockModel(String name) {
        generateSlabModel(name);
        generateTopSlabModel(name);
    }

    private static void generateSlabModel(String name) {
        String json = """
              {
                "parent": "minecraft:block/slab",
                "textures": {
                  "bottom": "genesis:block/%1$s",
                  "top": "genesis:block/%1$s",
                  "side": "genesis:block/%1$s"
                }
              }
              """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "assets/genesis/models/block/" + name + "_slab.json";

        FileWriter.writeFile(path, json);
    }

    private static void generateTopSlabModel(String name) {
        String json = """
              {
                "parent": "minecraft:block/slab_top",
                "textures": {
                  "bottom": "genesis:block/%1$s",
                  "top": "genesis:block/%1$s",
                  "side": "genesis:block/%1$s"
                }
              }
              """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "assets/genesis/models/block/" + name + "_slab_top.json";

        FileWriter.writeFile(path, json);
    }

    private static void generateBlockState(String name) {
        String json = """
                {
                  "variants": {
                    "type=bottom": {
                      "model": "genesis:block/%1$s_slab"
                    },
                    "type=top": {
                      "model": "genesis:block/%1$s_slab_top"
                    },
                    "type=double": {
                      "model": "genesis:block/%1$s"
                    }
                  }
                }
              """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "assets/genesis/blockstates/" + name + "_slab.json";

        FileWriter.writeFile(path, json);
    }

    private static void generateLootTable(String name) {
        String json = """
                {
                    "type": "minecraft:block",
                    "pools": [
                      {
                        "rolls": 1,
                        "entries": [
                          {
                            "type": "minecraft:item",
                            "name": "genesis:%1$s_slab",
                            "functions": [
                              {
                                "function": "minecraft:set_count",
                                "conditions": [
                                  {
                                    "condition": "minecraft:block_state_property",
                                    "block": "genesis:%1$s_slab",
                                    "properties": {
                                      "type": "double"
                                    }
                                  }
                                ],
                                "count": 2,
                                "add": false
                              },
                              {
                                "function": "minecraft:explosion_decay"
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
              
              """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "data/genesis/loot_tables/blocks/" + name + "_slab.json";

        FileWriter.writeFile(path, json);
    }
}
