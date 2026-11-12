package shipwrights.genesis.tests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.util.Mth;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.ChunkMapInvoker;
import shipwrights.genesis.space.VoidChunks;

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
        helper.assertTrue(here == Mth.clamp(serverViewDistance, 2, 32),
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
        LevelChunk chunk = space.getChunkAt(fresh);
        helper.assertTrue(VoidChunks.isVoid(space, chunk), "a new chunk of space isn't empty");
        var expected = ((FlatLevelSource) space.getChunkSource().getGenerator()).settings().getBiome()
                .unwrapKey().orElseThrow();
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

        // Never holds anything, so it's never on disk, however often this runs in the same world
        LevelChunk empty = space.getChunkAt(IN_SPACE.offset(-8192, 0, -8192));
        helper.assertTrue(!((VoidChunks.OnDisk) empty).genesis$isOnDisk(), "a chunk that was never saved is on disk");
        empty.setUnsaved(true);
        helper.assertTrue(!chunkMap.genesis$save(empty), "an empty chunk of space was saved");
        helper.assertTrue(!empty.isUnsaved(), "an empty chunk of space is still waiting to be saved");

        // Saved by this before, if it has run in this world, and then known to be on disk from being read
        BlockPos pos = IN_SPACE.offset(-8192, 0, -8192 + 16);
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
