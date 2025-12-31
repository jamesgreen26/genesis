package shipwrights.genesis.block.datagen;

public class StairGenerator {
    public static void generateStair(String name) {
        generateBlockState(name);
    }

    private static void generateBlockState(String name) {
        String json = """
                {
                  "variants": {
                    "facing=east,half=bottom,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 270
                    },
                    "facing=east,half=bottom,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner"
                    },
                    "facing=east,half=bottom,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 270
                    },
                    "facing=east,half=bottom,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer"
                    },
                    "facing=east,half=bottom,shape=straight": {
                      "model": "genesis:block/%1$s_stairs"
                    },
                    "facing=east,half=top,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180
                    },
                    "facing=east,half=top,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 90
                    },
                    "facing=east,half=top,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180
                    },
                    "facing=east,half=top,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 90
                    },
                    "facing=east,half=top,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "x": 180
                    },
                    "facing=north,half=bottom,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 180
                    },
                    "facing=north,half=bottom,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 270
                    },
                    "facing=north,half=bottom,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 180
                    },
                    "facing=north,half=bottom,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 270
                    },
                    "facing=north,half=bottom,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "y": 270
                    },
                    "facing=north,half=top,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 270
                    },
                    "facing=north,half=top,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180
                    },
                    "facing=north,half=top,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 270
                    },
                    "facing=north,half=top,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180
                    },
                    "facing=north,half=top,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "x": 180,
                      "y": 270
                    },
                    "facing=south,half=bottom,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner"
                    },
                    "facing=south,half=bottom,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 90
                    },
                    "facing=south,half=bottom,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer"
                    },
                    "facing=south,half=bottom,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 90
                    },
                    "facing=south,half=bottom,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "y": 90
                    },
                    "facing=south,half=top,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 90
                    },
                    "facing=south,half=top,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 180
                    },
                    "facing=south,half=top,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 90
                    },
                    "facing=south,half=top,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 180
                    },
                    "facing=south,half=top,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "x": 180,
                      "y": 90
                    },
                    "facing=west,half=bottom,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 90
                    },
                    "facing=west,half=bottom,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "y": 180
                    },
                    "facing=west,half=bottom,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 90
                    },
                    "facing=west,half=bottom,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "y": 180
                    },
                    "facing=west,half=bottom,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "y": 180
                    },
                    "facing=west,half=top,shape=inner_left": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 180
                    },
                    "facing=west,half=top,shape=inner_right": {
                      "model": "genesis:block/%1$s_stairs_inner",
                      "uvlock": true,
                      "x": 180,
                      "y": 270
                    },
                    "facing=west,half=top,shape=outer_left": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 180
                    },
                    "facing=west,half=top,shape=outer_right": {
                      "model": "genesis:block/%1$s_stairs_outer",
                      "uvlock": true,
                      "x": 180,
                      "y": 270
                    },
                    "facing=west,half=top,shape=straight": {
                      "model": "genesis:block/%1$s_stairs",
                      "uvlock": true,
                      "x": 180,
                      "y": 180
                    }
                  }
                }
              """.formatted(name);
        String path = BlockDataGenerator.FOLDER + "assets/genesis/blockstates/" + name + "_stairs.json";

        FileWriter.writeFile(path, json);
    }
}
