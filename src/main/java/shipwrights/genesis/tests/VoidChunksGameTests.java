package shipwrights.genesis.tests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.ChunkMapInvoker;
import shipwrights.genesis.space.VoidChunks;

import java.util.function.Consumer;

/// Space's chunks are kept cheap (see {@link VoidChunks}), and no other dimension's are touched
@GameTestHolder(GenesisMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class VoidChunksGameTests {

    private static final String TEMPLATE = "celestialtravelgametests.emptyplatform";

    /// Far from where other tests put things in space
    private static final BlockPos IN_SPACE = new BlockPos(-40_000, 64, -40_000);

    @GameTest(template = TEMPLATE)
    public static void spaceLoadsFewerChunks(GameTestHelper helper) {
        ServerLevel space = space(helper);
        int limit = GenesisCommonConfig.spaceViewDistance();
        int serverViewDistance = helper.getLevel().getServer().getPlayerList().getViewDistance();

        int inSpace = ((ChunkMapInvoker) space.getChunkSource().chunkMap).genesis$getServerViewDistance();
        helper.assertTrue(inSpace == Math.min(limit, Math.max(2, serverViewDistance)),
                "space's view distance is " + inSpace + ", with a limit of " + limit + " and the server's at "
                        + serverViewDistance);

        int here = ((ChunkMapInvoker) helper.getLevel().getChunkSource().chunkMap).genesis$getServerViewDistance();
        helper.assertTrue(here == Math.clamp(serverViewDistance, 2, 32),
                "the overworld's view distance is " + here + ", not the server's " + serverViewDistance);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyEmptySpaceIsVoid(GameTestHelper helper) {
        ServerLevel space = space(helper);
        LevelChunk chunk = space.getChunkAt(IN_SPACE);
        helper.assertTrue(VoidChunks.isVoid(space, chunk), "an empty chunk of space isn't void");

        space.setBlockAndUpdate(IN_SPACE, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!VoidChunks.isVoid(space, chunk), "a chunk of space holding a block is void");
        space.setBlockAndUpdate(IN_SPACE, Blocks.AIR.defaultBlockState());
        helper.assertTrue(VoidChunks.isVoid(space, chunk), "a chunk of space emptied again isn't void");

        ServerLevel overworld = helper.getLevel();
        // High above the test's platform, where the overworld has nothing either
        LevelChunk elsewhere = overworld.getChunkAt(helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(!VoidChunks.isVoid(overworld, elsewhere), "a chunk outside space is void");
        helper.succeed();
    }

    /// Space's chunks are made empty rather than generated, with the biome generating them would give
    @GameTest(template = TEMPLATE)
    public static void spaceIsMadeEmptyWithItsBiome(GameTestHelper helper) {
        ServerLevel space = space(helper);
        helper.assertTrue(VoidChunks.generatesNothing(space), "space is still generated");
        helper.assertTrue(!VoidChunks.generatesNothing(helper.getLevel()), "the overworld isn't generated");

        // Never loaded before, so it isn't on disk
        BlockPos fresh = IN_SPACE.offset(4096, 0, 4096);
        ChunkPos freshChunk = new ChunkPos(fresh);
        // Whether it was loaded, and whether as one newly generated
        boolean[] loaded = {false};
        boolean[] generated = {false};
        Consumer<ChunkEvent.Load> listener = event -> {
            if (event.getLevel() == space && event.getChunk().getPos().equals(freshChunk)) {
                loaded[0] = true;
                generated[0] = event.isNewChunk();
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        LevelChunk chunk;
        try {
            chunk = space.getChunkAt(fresh);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(loaded[0], "a new chunk of space was never loaded");
        helper.assertTrue(!generated[0], "a new chunk of space was generated");
        helper.assertTrue(VoidChunks.isVoid(space, chunk), "a new chunk of space isn't empty");
        var expected = ((FlatLevelSource) space.getChunkSource().getGenerator()).settings().getBiome();
        for (int y : new int[]{space.getMinBuildHeight(), 64, space.getMaxBuildHeight() - 1}) {
            var biome = space.getBiome(fresh.atY(y));
            helper.assertTrue(biome.is(expected), "a new chunk of space has the biome " + biome + " at " + y);
        }
        helper.succeed();
    }

    /// Empty space is never written, but once a chunk has been, it's written again when emptied, so what it held
    /// doesn't come back
    @GameTest(template = TEMPLATE)
    public static void emptySpaceIsOnlySavedOverWhatWasSaved(GameTestHelper helper) {
        ServerLevel space = space(helper);
        ChunkMapInvoker chunkMap = (ChunkMapInvoker) space.getChunkSource().chunkMap;

        // Never holds anything, so it's never on disk, however often this runs in the same world. If empty space is
        // ever saved, though, it's on disk in that world from then on, and has to be moved here once that's put right
        LevelChunk empty = space.getChunkAt(IN_SPACE.offset(-12288, 0, -12288));
        helper.assertTrue(!((VoidChunks.OnDisk) empty).genesis$isOnDisk(), "a chunk that was never saved is on disk");
        empty.setUnsaved(true);
        helper.assertTrue(!chunkMap.genesis$save(empty), "an empty chunk of space was saved");
        helper.assertTrue(!empty.isUnsaved(), "an empty chunk of space is still waiting to be saved");

        // Saved by this before, if it has run in this world, and then known to be on disk from being read
        BlockPos pos = IN_SPACE.offset(-12288, 0, -12288 + 16);
        LevelChunk chunk = space.getChunkAt(pos);
        space.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(chunkMap.genesis$save(chunk), "a chunk of space holding a block wasn't saved");
        helper.assertTrue(((VoidChunks.OnDisk) chunk).genesis$isOnDisk(), "a saved chunk isn't known to be on disk");

        space.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(chunkMap.genesis$save(chunk), "a saved chunk of space wasn't saved again once emptied");
        helper.succeed();
    }

    private static ServerLevel space(GameTestHelper helper) {
        ServerLevel space = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE));
        helper.assertTrue(space != null, "no space dimension");
        return space;
    }
}
