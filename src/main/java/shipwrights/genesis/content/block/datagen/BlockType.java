package shipwrights.genesis.content.block.datagen;

import java.util.Set;

public enum BlockType {
    simple, slab, stairs, pillar, fence, wall;

    public static final Set<String> suffixes = Set.of(
            "_slab",
            "_stairs",
            "_pillar",
            "_fence",
            "_wall"
    );
}
